/*****************************************************************************************[Alloc.h]
Copyright (c) 2008-2010, Niklas Sorensson

Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
associated documentation files (the "Software"), to deal in the Software without restriction,
including without limitation the rights to use, copy, modify, merge, publish, distribute,
sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all copies or
substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT
NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT
OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
**************************************************************************************************/


#ifndef Minisat_Alloc_h
#define Minisat_Alloc_h

#include "mtl/XAlloc.h"
#include "mtl/Vec.h"

namespace Minisat {

//=================================================================================================
// Simple Region-based memory allocator:

template<class T>
class RegionAllocator
{
    T*        memory;
    uint32_t  sz;
    uint32_t  cap;
    uint32_t  wasted_;

    void capacity(uint32_t min_cap);

 public:
    // TODO: make this a class for better type-checking?
    typedef uint32_t Ref;
    enum { Ref_Undef = UINT32_MAX };
    enum { Unit_Size = sizeof(uint32_t) };

    explicit RegionAllocator(uint32_t start_cap = 1024*1024) : memory(NULL), sz(0), cap(0), wasted_(0){ capacity(start_cap); }
    ~RegionAllocator()
    {
        if (memory != NULL)
            ::free(memory);
    }


    uint32_t size      () const      { return sz; }
    uint32_t wasted    () const      { return wasted_; }

    Ref      alloc     (int size); 
    void     free      (int size)    { wasted_ += size; }

    // Deref, Load Effective Address (LEA), Inverse of LEA (AEL):
    T&       operator[](Ref r)       { assert(r >= 0 && r < sz); return memory[r]; }
    const T& operator[](Ref r) const { assert(r >= 0 && r < sz); return memory[r]; }

    T*       lea       (Ref r)       { assert(r >= 0 && r < sz); return &memory[r]; }
    const T* lea       (Ref r) const { assert(r >= 0 && r < sz); return &memory[r]; }
    Ref      ael       (const T* t)  {
        if ((void*)t >= (void*)&memory[0] && (void*)t < (void*)&memory[sz-1]) {
            return  (Ref)(t - &memory[0]);
        }
        return Ref_Undef;
    }

    void     moveTo(RegionAllocator& to) {
        if (to.memory != NULL) ::free(to.memory);
        to.memory = memory;
        to.sz = sz;
        to.cap = cap;
        to.wasted_ = wasted_;

        memory = NULL;
        sz = cap = wasted_ = 0;
    }

    void     copyTo(RegionAllocator& to) const {
        if (to.memory != NULL) ::free(to.memory);
        to.memory = NULL;
        to.memory = (T*)xrealloc(to.memory, sizeof(T)*cap);
        for (int i = 0; i < cap; i++) {
            to.memory[i] = memory[i];
        }
        to.sz = sz;
        to.cap = cap;
        to.wasted_ = wasted_;
    }

};

template<class T>
void RegionAllocator<T>::capacity(uint32_t min_cap)
{
    if (cap >= min_cap) return;

    uint32_t prev_cap = cap;
    while (cap < min_cap){
        // NOTE: Multiply by a factor (13/8) without causing overflow, then add 2 and make the
        // result even by clearing the least significant bit. The resulting sequence of capacities
        // is carefully chosen to hit a maximum capacity that is close to the '2^32-1' limit when
        // using 'uint32_t' as indices so that as much as possible of this space can be used.
        uint32_t delta = ((cap >> 1) + (cap >> 3) + 2) & ~1;
        cap += delta;

        if (cap <= prev_cap)
            throw OutOfMemoryException();
    }
    // printf(" .. (%p) cap = %u\n", this, cap);

    assert(cap > 0);
    memory = (T*)xrealloc(memory, sizeof(T)*cap);
}


template<class T>
typename RegionAllocator<T>::Ref
RegionAllocator<T>::alloc(int size)
{ 
    // printf("ALLOC called (this = %p, size = %d)\n", this, size); fflush(stdout);
    assert(size > 0);
    capacity(sz + size);

    uint32_t prev_sz = sz;
    sz += size;
    
    // Handle overflow:
    if (sz < prev_sz)
        throw OutOfMemoryException();

    return prev_sz;
}


//=================================================================================================
// Simple Generational Region-based memory allocator:

template<class T>
class GenerationalRegionAllocator
{
    static const uint32_t GENERATION_BITS = 8;
    static const uint32_t GENERATION_COUNT = (1 << GENERATION_BITS);

    union InnerRef {
        typename RegionAllocator<T>::Ref ref;
        struct {
            uint32_t generation : GENERATION_BITS;
            uint32_t index : (sizeof(uint32_t) * 8 - GENERATION_BITS);
        } fields;
    };

    class Generation {
        uint32_t ref_count;
        
        RegionAllocator<T> allocator;

    public:

        typedef typename RegionAllocator<T>::Ref Ref;

        Generation(uint32_t start_cap) :
            ref_count(1),
            allocator(start_cap)
        {
        }

        ~Generation() {
        }

        void retain() { __sync_fetch_and_add(&ref_count, 1); }
        void release() {
            if (__sync_fetch_and_sub(&ref_count, 1) == 1) {
              delete this;
            }
        };

        Ref alloc(int size) { return allocator.alloc(size); }
    
        T&       operator[](Ref r)       {
            return allocator[r];
        }

        const T& operator[](Ref r) const {
            return allocator[r];
        }

        T*       lea       (Ref r)       {
            return allocator.lea(r);
        }

        const T* lea       (Ref r) const {
            return allocator.lea(r);
        }

        Ref      ael       (const T* t)  {
            return allocator.ael(t);
        }
    };

    Generation* generations[GENERATION_COUNT];
    uint32_t current_generation;
    uint32_t start_cap;
    uint32_t size_;
    uint32_t wasted_;

 public:
    typedef typename RegionAllocator<T>::Ref Ref;
    enum { Ref_Undef = UINT32_MAX };
    enum { Unit_Size = sizeof(uint32_t) };

    explicit GenerationalRegionAllocator(uint32_t start_cap = 1024*1024) :
        current_generation(0),
        start_cap(start_cap),
        size_(0),
        wasted_(0)
    {
        for (uint32_t index = 0; index < GENERATION_COUNT; index += 1) {
          generations[index] = NULL;
        }

        generations[0] = new Generation(start_cap);
    }

    ~GenerationalRegionAllocator()
    {
        for (uint32_t index = 0; index < GENERATION_COUNT; index += 1) {
            if (generations[index] != NULL) {
              generations[index]->release();
              generations[index] = NULL;
            }
        }
    }

    uint32_t size      () const {
        return size_;
    }

    uint32_t wasted    () const {
        return wasted_;
    }

    Ref      alloc     (int size) {
      Generation* generation = generations[current_generation]; 
      InnerRef ref = {0};
      ref.ref = generation->alloc(size);

      if (ref.fields.generation != 0) {
        throw OutOfMemoryException();
      }
    
      this->size_ += size;
      
      ref.fields.generation = current_generation;
      return ref.ref;
    }; 

    void     free      (int size)    { this->wasted_ += size; }

    // Deref, Load Effective Address (LEA), Inverse of LEA (AEL):
    T&       operator[](Ref r)       {
        InnerRef ref;
        ref.ref = r;
        
        Generation* generation = generations[ref.fields.generation];
        ref.fields.generation = 0;
      
        return (*generation)[ref.ref]; 
    }
    const T& operator[](Ref r) const {
        InnerRef ref;
        ref.ref = r;
        
        Generation* generation = generations[ref.fields.generation];
        ref.fields.generation = 0;
      
        return (*generation)[ref.ref]; 
    }

    T*       lea       (Ref r)       {
        InnerRef ref;
        ref.ref = r;

        Generation* generation = generations[ref.fields.generation];
        ref.fields.generation = 0;

        return generation->lea(ref.ref);
    }
    const T* lea       (Ref r) const {
        InnerRef ref;
        ref.ref = r;

        Generation* generation = generations[ref.fields.generation];
        ref.fields.generation = 0;

        return generation->lea(ref.ref);
    }
    Ref      ael       (const T* t)  {
        InnerRef ref;
        for (uint32_t generation = 0;
             generation < GENERATION_COUNT &&
             generations[generation] != NULL;
             generation += 1)
        {
            ref.ref = generations[generation]->ael(t); 
            if (ref.ref != UINT32_MAX) {
                ref.fields.generation = generation;
                return ref.ref;
            }
        }

        ref.ref = UINT32_MAX;
        return ref.ref;
    }

    void     moveTo(GenerationalRegionAllocator& to) {
        for (uint32_t generation = 0;
             generation < GENERATION_COUNT;
             generation += 1)
        {
            if (to.generations[generation] != NULL) {
                to.generations[generation]->release();
                to.generations[generation] = NULL;
            }

            to.generations[generation] = this->generations[generation];
            this->generations[generation] = NULL;
        }

        to.size_ = size_;
        to.wasted_ = wasted_;
        to.current_generation = current_generation;

        size_ = wasted_ = current_generation = 0;
    }

    void     copyTo(GenerationalRegionAllocator& to) {
        for (uint32_t generation = 0;
             generation < GENERATION_COUNT;
             generation += 1)
        {
            if (to.generations[generation] != NULL) {
                to.generations[generation]->release();
                to.generations[generation] = NULL;
            }

            to.generations[generation] = this->generations[generation];
            if (this->generations[generation] != NULL) {
              this->generations[generation]->retain();
            }
        }

        current_generation += 1;

        this->generations[current_generation] = new Generation(start_cap);
        to.generations[current_generation] = new Generation(to.start_cap);

        to.size_ = size_;
        to.wasted_ = wasted_;
        to.current_generation = current_generation;
    }

};

}

#endif
