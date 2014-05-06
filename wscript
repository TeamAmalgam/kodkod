#! /usr/bin/env python
# encoding: utf-8

import os
import os.path
import sys
from waflib import Options
import urllib2
import hashlib

APPNAME = 'kodkod'
VERSION = '2.0'

def options(opt):
    solver_options = opt.add_option_group('solver options')
    solver_options.add_option('--no-solvers', dest='build_solvers', default=True, action='store_false', help="skips building native SAT solvers")

    test_options = opt.add_option_group('test options')
    test_options.add_option('--skip-tests', dest='run_tests', default=True, action='store_false', help="skips running tests")

    opt.recurse('src lib tests')

def deps(ctx):
    ctx.recurse('tests')
    ctx.install_deps('deps')

def configure(conf):
    conf.env.DEPS_DIR = os.path.abspath('./deps')
    conf.recurse('src lib tests')

def build(bld):
    if not bld.variant:
        bld.recurse('src')
    if Options.options.build_solvers:
        bld.recurse('lib')

def test_build(bld):
    bld.recurse('tests')

def dist(dst):
    dst.base_name = APPNAME + '-' + VERSION
    dst.algo      = 'zip'
    dst.excl      = '**/.* **/*~ **/*.pyc **/*.swp **/CVS/** **/taglet/**'
    dst.files     = dst.path.ant_glob('LICENSE NEWS MANIFEST wscript src/** lib/** tests/**', excl=dst.excl)

def test(tst):
    tst.recurse('tests')

def all(ctx):
    new_commands = [
        'build',
        'install']

    if Options.options.run_tests:
        new_commands += [
            'test_build',
            'test']

    new_commands += ['dist']
    Options.commands = new_commands + Options.commands

from waflib.Build import BuildContext
class TestBuildContext(BuildContext):
    '''builds the project's tests'''
    cmd = 'test_build'
    fun = 'test_build'

class TestContext(BuildContext):
    '''runs the project's tests'''
    cmd = 'test'
    fun = 'test'

from waflib.Context import Context
class DepsContext(Context):
    '''downloads project dependencies'''
    cmd = 'deps'
    fun = 'deps'
    urls = {}
    hashes = {}

    def add_dep(self, file, url, sha256_hash):
        self.urls[file] = url
        self.hashes[file] = sha256_hash

    def install_deps(self, deps_dir):
        if not os.path.exists(deps_dir):
          os.makedirs(deps_dir)

        deps_dir = os.path.abspath(deps_dir)
        for file, url in self.urls.iteritems():
            download_path = os.path.join(deps_dir, file)

            self.to_log("Downloading " + file + " from " + url + "\n")

            # Download the file from the specified url.
            request = urllib2.urlopen(url)
            data = request.read();

            # Calculate the SHA-256 hash of the file.
            actual_sha2 = hashlib.sha256(data).hexdigest()
            expected_sha2 = self.hashes[file]

            if expected_sha2 == actual_sha2 :
                file_handle = open(download_path, "wb")
                file_handle.write(data)
                file_handle.close()
            else :
                self.fatal('Failed to validate ' + file + ' expected hash "' + expected_sha2 + '" got "' + actual_sha2 + '".')
