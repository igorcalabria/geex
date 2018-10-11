package geex;

import java.util.ArrayList;
import java.util.Stack;
import java.util.HashMap;
import geex.Seed;
import geex.SeedUtils;
import geex.Counter;
import java.lang.RuntimeException;
import geex.LocalVars;
import geex.Binding;

public class State {

    private ArrayList<Seed> _lowerSeeds = new ArrayList<Seed>();
    private ArrayList<Seed> _upperSeeds = new ArrayList<Seed>();
    private Mode _maxMode = Mode.Pure;
    private Object _output = null;
    private ArrayList<Seed> _dependingScopes = new ArrayList<Seed>();
    private LocalVars _lvars = new LocalVars();
    private StateSettings _settings = null;


    Stack<Integer> _scopeStack = new Stack<Integer>();
    HashMap<Object, Seed> _seedCache = new HashMap<Object, Seed>();
    ArrayList<HashMap<Object, Seed>> _seedCacheStack 
        = new ArrayList<HashMap<Object, Seed>>();
    Stack<Mode> _modeStack = new Stack<Mode>();
    
    public State(StateSettings s) {
        if (s == null) {
            throw new RuntimeException("No settings provided");
        }
        s.check();
        _settings = s;
    }

    public void beginScope(Seed s, boolean isDepending) {
        int id = s.getId();
        _scopeStack.add(id);
        if (isDepending) {
            _dependingScopes.add(s);
        }
        _modeStack.add(_maxMode);
        _seedCacheStack.add(_seedCache);
        _seedCache = new HashMap<Object, Seed>();
        _maxMode = Mode.Pure;
    }

    public LocalVars localVars() {
        return _lvars;
    }


    private class StateCallbackWrapper extends AFn {
        private StateCallback _cb;
        
        public StateCallbackWrapper(StateCallback cb) {
            if (cb == null) {
                throw new RuntimeException(
                    "StateCallback cannot be null");
            }
            _cb = cb;
        }
        
        public Object invoke(Object x) {
            if (x == null) {
                throw new RuntimeException("StateCallbackWrapper got a null pointer");
            }
            if (!(x instanceof State)) {
                throw new RuntimeException(
                    "StateCallbackWrapper did not receive a valid state" + x.toString());
            }
            return _cb.call((State)x);
        }
    }

    private StateCallbackWrapper wrapCallback(StateCallback cb) {
        return new StateCallbackWrapper(cb);
    }

    public Object getPlatform() {
        return _settings.platform;
    }

    public int getLower() {
        return -_lowerSeeds.size();
    }

    public int getUpper() {
        return _upperSeeds.size();
    }
    
    int nextLowerIndex() {
        return _lowerSeeds.size()-1;
    }

    int nextUpperIndex() {
        return _upperSeeds.size();
    }


    public void addSeed(Seed x) {
        if (SeedUtils.isRegistered(x)) {
            throw new RuntimeException(
                "Cannot add seed with id "
                + x.getId() + " because it is already registered");
        }
        x.setId(nextUpperIndex());
        _maxMode = SeedUtils.max(_maxMode, x.getMode());
        _upperSeeds.add(x);
    }

    public Seed getSeed(int index) {
        if (0 <= index) {
            return _upperSeeds.get(index);
        }
        return _lowerSeeds.get(-index-1);
    }

    public void setOutput(Object o) {
        _output = o;
    }

    public Object getOutput() {
        return _output;
    }

    public void addDependenciesFromDependingScopes(Seed dst) {
        for (int i = 0; i < _dependingScopes.size(); i++) {
            Seed from = _dependingScopes.get(i);
            if (from.getId() > dst.getId()) {
                from.deps().addGenKey(dst);
            }
        }
    }

    public int getSeedCount() {
        return _upperSeeds.size() + _lowerSeeds.size();
    }

  
    /*build-referents
  build-ids-to-visit
  check-referent-visibility
  check-scope-stacks*/

    private void buildReferents() {
        int lower = getLower();
        int upper = getUpper();
        for (int i = lower; i < upper; i++) {
            Seed seed = getSeed(i);
            int id = seed.getId();
            seed.deps().addReferentsFromId(id);
        }
    }

    public void finalizeState() {
        buildReferents();
    }

    public void disp() {
        System.out.println("=== State ===");
        int lower = getLower();
        int upper = getUpper();
        for (int i = lower; i < upper; i++) {
            Seed seed = getSeed(i);
            System.out.println(
                String.format(
                    " - %4d %s",
                    i, seed.toString()));
            seed.deps().disp();
            seed.refs().disp();
        }
    }

    private Seed advanceToNextSeed(int index) {
        while (index < getUpper()) {
            Seed seed = getSeed(index);
            if (seed != null) {
                return seed;
            }
        }        
        return null;
    }

    private boolean shouldBindResult(Seed seed) {
        Boolean b = seed.shouldBind();
        int refCount = seed.refs().count();
        if (b == null) {
            if (SeedFunction.Begin == seed.getSeedFunction()) {
                return false;
            } else {
                switch (seed.getMode()) {
                case Pure: return 2 <= refCount;
                case Ordered: return 1 <= refCount;
                case SideEffectful: return true;
                }
                return true;
            }
        } else {
            return b.booleanValue();
        }
    }

    private void bind(Seed seed) {
        if (!SeedUtils.hasCompilationResult(seed)) {
            throw new RuntimeException(
                "Cannot bind a seed before it has a result (seed "
                + seed.toString() + ")");
        }

        Object result = seed.getCompilationResult();
        Binding b = _lvars.addBinding(seed);
        seed.setCompilationResult(
            _settings
            .platformFunctions
            .renderLocalVarName(b.varName));
    }

    private void maybeBind(Seed seed) {
        if (shouldBindResult(seed)) {
            bind(seed);
        }
    }

    private Object generateCodeFrom(
        Object lastResult, int index) {
        Seed seed = advanceToNextSeed(index);
        if (seed == null) {
            return lastResult;
        } else if (SeedUtils.hasCompilationResult(seed)) {
            return generateCodeFrom(
                seed.getCompilationResult(),
                index+1);
        }
        
        final Counter wasCalled = new Counter();
        StateCallback innerCallback = new StateCallback() {
                public Object call(State state) {
                    if (!SeedUtils.hasCompilationResult(seed)) {
                        throw new RuntimeException(
                            "No compilation result set for seed "
                            + seed.toString());
                    }
                    maybeBind(seed);
                    wasCalled.step();
                    Object result = seed.getCompilationResult();
                    if (result instanceof Seed) {
                        throw new RuntimeException(
                            "The result of '" + seed 
                            + "' is a seed'");
                    }
                    System.out.println("inner result is=" + result);

                    if (seed.getSeedFunction() == SeedFunction.End) {
                        return result;
                    }
                    return generateCodeFrom(
                        result,
                        index+1);
                }
            };

        Object result = seed.compile(this, wrapCallback(
                innerCallback));

        System.out.println(
            "Result of seed " + seed + " is " + result);

        if (wasCalled.get() == 0) {
            throw new RuntimeException(
                "Callback never called when compiling seed "
                + seed.toString());
        }

        return result;
    }

    public Object generateCode() {
        return generateCodeFrom(null, getLower());
    }
}
