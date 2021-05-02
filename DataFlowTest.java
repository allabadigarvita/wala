import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.classLoader.Language;
import com.ibm.wala.core.tests.callGraph.CallGraphTestUtil;
import com.ibm.wala.core.tests.util.TestConstants;
import com.ibm.wala.core.tests.util.WalaTestCase;
import com.ibm.wala.dataflow.IFDS.ISupergraph;
import com.ibm.wala.dataflow.IFDS.TabulationResult;
import com.ibm.wala.dataflow.graph.BitVectorSolver;
import com.ibm.wala.ipa.callgraph.AnalysisCacheImpl;
import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.CallGraphBuilder;
import com.ibm.wala.ipa.callgraph.CallGraphBuilderCancelException;
import com.ibm.wala.ipa.callgraph.Entrypoint;
import com.ibm.wala.ipa.callgraph.IAnalysisCacheView;
import com.ibm.wala.ipa.callgraph.impl.Everywhere;
import com.ibm.wala.ipa.callgraph.impl.Util;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.cfg.BasicBlockInContext;
import com.ibm.wala.ipa.cfg.ExplodedInterproceduralCFG;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.ipa.cha.ClassHierarchyFactory;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.ISSABasicBlock;
import com.ibm.wala.ssa.SSACFG;
import com.ibm.wala.ssa.SSAOptions;
import com.ibm.wala.ssa.analysis.ExplodedControlFlowGraph;
import com.ibm.wala.ssa.analysis.IExplodedBasicBlock;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.collections.Pair;
import com.ibm.wala.util.config.AnalysisScopeReader;
import com.ibm.wala.util.config.FileOfClasses;
import com.ibm.wala.util.intset.IntIterator;
import com.ibm.wala.util.intset.IntSet;
import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class DataflowTest extends WalaTestCase {

  private static AnalysisScope scope;

  private static IClassHierarchy cha;

  private static final String EXCLUSIONS =
      "java\\/awt\\/.*\n"
          + "javax\\/swing\\/.*\n"
          + "sun\\/awt\\/.*\n"
          + "sun\\/swing\\/.*\n"
          + "com\\/sun\\/.*\n"
          + "sun\\/.*\n"
          + "org\\/netbeans\\/.*\n"
          + "org\\/openide\\/.*\n"
          + "com\\/ibm\\/crypto\\/.*\n"
          + "com\\/ibm\\/security\\/.*\n"
          + "org\\/apache\\/xerces\\/.*\n"
          + "java\\/security\\/.*\n"
          + "";

  @BeforeClass
  public static void beforeClass() throws Exception {

    scope =
        AnalysisScopeReader.readJavaScope(
            TestConstants.WALA_TESTDATA, null, DataflowTest.class.getClassLoader());

    scope.setExclusions(new FileOfClasses(new ByteArrayInputStream(EXCLUSIONS.getBytes("UTF-8"))));
    try {
      cha = ClassHierarchyFactory.make(scope);
    } catch (ClassHierarchyException e) {
      throw new Exception(e);
    }
  }

  /*
   * (non-Javadoc)
   *
   * @see junit.framework.TestCase#tearDown()
   */
  @AfterClass
  public static void afterClass() throws Exception {
    scope = null;
    cha = null;
  }


  @Test
  public void testReachingDefs() throws CancelException {
    IAnalysisCacheView cache = new AnalysisCacheImpl();
    final MethodReference ref =
        MethodReference.findOrCreate(
            ClassLoaderReference.Application, "Dataflow", "testrd", "()V");
    IMethod method = cha.resolveMethod(ref);
    IR ir = cache.getIRFactory().makeIR(method, Everywhere.EVERYWHERE, SSAOptions.defaultOptions());
    SSACFG cfg = ir.getControlFlowGraph();
    ReachingDefsAnalysis reachingDefs = new ReachingDefsAnalysis(cfg, cha);
    BitVectorSolver<ISSABasicBlock> solver = reachingDefs.analyze();
    for (ISSABasicBlock ebb : cfg) {
      if (ebb.getNumber() == 4) {
        IntSet out = solver.getOut(ebb).getValue();
        Assert.assertEquals(3, out.size());
        Assert.assertTrue(out.contains(2));
      }
    }
  }

}
