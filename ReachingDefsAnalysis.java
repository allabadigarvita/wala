import com.ibm.wala.classLoader.IField;
import com.ibm.wala.dataflow.graph.AbstractMeetOperator;
import com.ibm.wala.dataflow.graph.BitVectorFramework;
import com.ibm.wala.dataflow.graph.BitVectorIdentity;
import com.ibm.wala.dataflow.graph.BitVectorKillGen;
import com.ibm.wala.dataflow.graph.BitVectorSolver;
import com.ibm.wala.dataflow.graph.BitVectorUnion;
import com.ibm.wala.dataflow.graph.ITransferFunctionProvider;
import com.ibm.wala.fixpoint.BitVectorVariable;
import com.ibm.wala.fixpoint.UnaryOperator;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.ssa.ISSABasicBlock;
import com.ibm.wala.ssa.SSACFG;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSAPutInstruction;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.collections.HashMapFactory;
import com.ibm.wala.util.collections.ObjectArrayMapping;
import com.ibm.wala.util.intset.BitVector;
import com.ibm.wala.util.intset.OrdinalSetMapping;
import java.util.ArrayList;
import java.util.Map;

public class ReachingDefsAnalysis {

  private final SSACFG cfg;
  private final IClassHierarchy cha;

  /**
   * maps the index of a putstatic IR instruction to a more compact numbering for use in bitvectors
   */
  private final OrdinalSetMapping<Integer> putInstrNumbering;


  /**
   * maps each static field to the numbers of the statements (in {@link #putInstrNumbering}) that
   * define it; used for kills in flow functions
   */
  private final Map<IField, BitVector> staticField2DefStatements = HashMapFactory.make();

  public ReachingDefsAnalysis(SSACFG ecfg, IClassHierarchy cha)
  {
    this.cfg = ecfg;
    this.cha = cha;
    this.putInstrNumbering = numberPutStatics();
  }

  /** generate a numbering of the putstatic instructions */
  private OrdinalSetMapping<Integer> numberPutStatics()
  {
    ArrayList<Integer> putInstrs = new ArrayList<>();
    SSAInstruction[] instructions = cfg.getInstructions();
    for (int i = 0; i < instructions.length; i++) {
      SSAInstruction instruction = instructions[i];
      if (instruction instanceof SSAPutInstruction
          && ((SSAPutInstruction) instruction).isStatic())
      {
        SSAPutInstruction putInstr = (SSAPutInstruction) instruction;
        int instrNum = putInstrs.size();
        putInstrs.add(instruction.iIndex());
        IField field = cha.resolveField(putInstr.getDeclaredField());
        assert field != null;
        BitVector bv = staticField2DefStatements.get(field);
        if (bv == null)
        {
          bv = new BitVector();
          staticField2DefStatements.put(field, bv);
        }
        bv.set(instrNum);
      }
    }
    OrdinalSetMapping<Integer> map =  new ObjectArrayMapping<>(putInstrs.toArray(new Integer[0]));
    return map;
  }

  private class TransferFunctions implements ITransferFunctionProvider<ISSABasicBlock, BitVectorVariable>
  {
    @Override
    public UnaryOperator<BitVectorVariable> getNodeTransferFunction(ISSABasicBlock node)
    {
      for(SSAInstruction inst: node)
      {
        int instructionIndex = inst.iIndex();
        if (inst instanceof SSAPutInstruction
            && ((SSAPutInstruction) inst).isStatic())
        {
          // kill all defs of the same static field, and gen this instruction
          final SSAPutInstruction putInstr = (SSAPutInstruction) inst;
          final IField field = cha.resolveField(putInstr.getDeclaredField());
          assert field != null;
          BitVector kill = staticField2DefStatements.get(field);
          BitVector gen = new BitVector();
          gen.set(putInstrNumbering.getMappedIndex(instructionIndex));
          return new BitVectorKillGen(kill, gen);
        } else {
          // identity function for non-putstatic instructions
          return BitVectorIdentity.instance();
        }
      }
      return BitVectorIdentity.instance();
    }

    @Override
    public AbstractMeetOperator<BitVectorVariable> getMeetOperator()
    {
      return BitVectorUnion.instance();
    }

    @Override
    public boolean hasEdgeTransferFunctions()
    {
      return false;
    }

    @Override
    public boolean hasNodeTransferFunctions()
    {
      return true;
    }

    @Override
    public UnaryOperator<BitVectorVariable> getEdgeTransferFunction(ISSABasicBlock src,
        ISSABasicBlock dst)
    {
      throw new UnsupportedOperationException();
    }
  }

  public BitVectorSolver<ISSABasicBlock> analyze() throws CancelException
  {
    BitVectorFramework<ISSABasicBlock, Integer> framework = new BitVectorFramework<>(cfg, new TransferFunctions(), putInstrNumbering);
    BitVectorSolver<ISSABasicBlock> solver = new BitVectorSolver<>(framework);
    solver.solve(null);

    for (ISSABasicBlock bb : cfg)
    {
      System.out.println("BB: " + bb.getNumber());

      for(SSAInstruction inst: bb)
      {
        System.out.println("inst: " + inst);
      }
      System.out.println("In" + solver.getIn(bb));
      System.out.println("Out" + solver.getOut(bb));

    }

    return solver;
  }
}
