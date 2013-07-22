package kodkod.engine;

import kodkod.ast.Formula;
import kodkod.engine.config.Options;
import kodkod.engine.fol2sat.HigherOrderDeclException;
import kodkod.engine.fol2sat.UnboundLeafException;
import kodkod.instance.Bounds;
import kodkod.instance.Instance;
import com.microsoft.z3.Context;
import com.microsoft.z3.BoolExpr;
import com.microsoft.z3.Solver;
import com.microsoft.z3.Z3Exception;
import com.microsoft.z3.Status;
import com.microsoft.z3.Model;

public final class Z3Solver implements KodkodSolver {
    private Context context;
    private final Options options;

    public Z3Solver() {
        try {
            context = new Context();
        } catch (Z3Exception e) {
            throw new RuntimeException(e);
        }
        
        this.options = new Options();
    }

    public Options options() {
        return this.options;
    }
    public Solution solve(Formula formula, Bounds bounds)
    throws HigherOrderDeclException, UnboundLeafException, AbortedException {
        try {
            Z3Translator translator = new Z3Translator(context, formula, bounds);
            translator.generateTranslation();
            BoolExpr expression = translator.getTranslation();
            Solver solver = context.MkSolver();
            solver.Assert(expression);
            Status result = solver.Check();
            Statistics stats = new Statistics(0, 0, 0, 0, 0);
            
            if (result == Status.SATISFIABLE) {
                Model model = solver.Model();
                Instance instance = translator.getInstance(model);
                return Solution.satisfiable(stats, instance);
            } else if (result == Status.UNSATISFIABLE) {
                return Solution.unsatisfiable(stats, null);
            } else {
                throw new RuntimeException("Z3 Check return UNKNOWN.");
            }
        } catch (Z3Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void free() {
        context = null;
    }
}