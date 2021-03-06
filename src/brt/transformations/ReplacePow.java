package brt.transformations;

import claw.shenron.transformation.Transformation;
import claw.shenron.translator.Translator;
import claw.tatsu.xcodeml.exception.IllegalTransformationException;
import claw.tatsu.xcodeml.xnode.Xname;
import claw.tatsu.xcodeml.xnode.XnodeUtil;
import claw.tatsu.xcodeml.xnode.common.Xattr;
import claw.tatsu.xcodeml.xnode.common.Xcode;
import claw.tatsu.xcodeml.xnode.common.XcodeProgram;
import claw.tatsu.xcodeml.xnode.common.Xnode;
import claw.tatsu.xcodeml.xnode.fortran.FfunctionType;
import claw.tatsu.xcodeml.xnode.fortran.FortranType;
import claw.tatsu.xcodeml.xnode.fortran.FbasicType;
import claw.wani.transformation.ClawTransformation;
import claw.wani.x2t.configuration.Configuration;
import claw.tatsu.xcodeml.xnode.fortran.FmoduleDefinition;
import claw.tatsu.xcodeml.xnode.fortran.FfunctionDefinition;

import java.util.HashSet;
import java.util.List;
import java.util.ArrayList;
import java.util.Optional;
import java.util.Set;

/**
 * Replace the usage of the exponentiation operator (**) by a function call and
 * add the USE statement to import the module containing the function. The name
 * of the function and the module are a class fields.
 *
 * Created by Christophe Charpilloz on 11.16.18.
 */
public class ReplacePow extends ClawTransformation {
  private final String usageModuleName;
  private final String powFunctionName ;

  public ReplacePow() {
    super();
    usageModuleName = Configuration.get().getParameter("br_power_module_name");
    powFunctionName =
      Configuration.get().getParameter("br_power_function_name");
  }

  @Override
  public boolean analyze(XcodeProgram xcodeml, Translator translator) {
    return true;
  }

  @Override
  public boolean canBeTransformedWith(XcodeProgram xcodeml,
                                      Transformation other)
  {
    return false;
  }

  @Override
  public void transform(XcodeProgram xcodeml, Translator translator,
                        Transformation transformation)
                        throws IllegalTransformationException
  {
    Set<Xnode> modModules = new HashSet<>();
    // get the exponentiation operator (**) usage
    List<Xnode> fPowers = xcodeml.matchAll(Xcode.F_POWER_EXPR);
    List<Xnode> fPowersToKill = new ArrayList<>();
    FfunctionType fctType = addDummyFctType(xcodeml);
    
    // get dummy function types in case we need to replace an
    // operator by a function call
    for (Xnode fPow : fPowers) {
      ModuleHelper.addUse(fPow, usageModuleName, xcodeml);

      if (! isInDecl(fPow)) {
        fPowersToKill.add(fPow);
      }
    }

    for (Xnode fPow : fPowersToKill) {
      replaceExponentiation(fPow, xcodeml, fctType);
      XnodeUtil.safeDelete(fPow);
    }
  }

  private boolean isInDecl(Xnode node) {
    return node.matchAncestor(Xcode.DECLARATIONS) != null;
  }

  /**
   * Return a dummy function type that is needed when adding a function call
   * that is part of and imported (USE) module.
   *
   * @param  xcodeml The current context.
   * @return The dummy function type as a String.
   */
  private FfunctionType addDummyFctType(XcodeProgram xcodeml) {
    FfunctionType dummyFctType = xcodeml.createFunctionType(Xname.TYPE_F_REAL);
    xcodeml.getTypeTable().add(dummyFctType);
    return dummyFctType;
  }

  /**
   * Replace the usage of the exponentiation operator node by a function call
   * node.
   *
   * @param fPow    the ** operator node.
   * @param xcodeml the context.
   * @param fctType a dummy function type.
   * @throws IllegalTransformationException
   */
  private void replaceExponentiation(Xnode fPow, XcodeProgram xcodeml,
                                     FfunctionType fctType)
                                     throws IllegalTransformationException
  {
    int nChildren = fPow.children().size();
    if (nChildren != 2) {
      throw new IllegalTransformationException(
        "Unexpected number of arguments: " + nChildren, fPow.lineNo());
    }

    Xnode functionCall = xcodeml.createFctCall(Xname.TYPE_F_REAL,
      powFunctionName, fctType.getType());
    Xnode argument = functionCall.matchDescendant(Xcode.ARGUMENTS);
    argument.append(fPow.child(0), true);
    argument.append(fPow.child(1), true);
    fPow.insertAfter(functionCall);
  }
}
