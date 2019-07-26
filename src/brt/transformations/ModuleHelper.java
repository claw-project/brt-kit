package brt.transformations;

import claw.tatsu.xcodeml.xnode.common.Xattr;
import claw.tatsu.xcodeml.xnode.common.Xcode;
import claw.tatsu.xcodeml.xnode.common.XcodeProgram;
import claw.tatsu.xcodeml.xnode.common.Xnode;
import claw.tatsu.xcodeml.xnode.fortran.FmoduleDefinition;
import claw.tatsu.xcodeml.xnode.fortran.FfunctionDefinition;
import claw.tatsu.xcodeml.exception.IllegalTransformationException;

import java.util.Collection;
import java.util.Optional;

public class ModuleHelper {
  /**
   * Return the current module or program of the current node.
   *
   * @param  node The current node.
   * @return The module containing the node as an Xnode.
   */
  public static Optional<Xnode> getModule(Xnode node) {
    Xnode module = node.findParentModule();
    // if null then find PROGRAM, FUNCTION or SUBROUTINE node
    if (module == null) {
      Xnode pfs = node.findParentFunction();
      // if null then there is no PROGRAM or MODULE
      if (pfs == null) {
        return Optional.empty();
      } else {
        return Optional.of(pfs);
      }
    } else {
      return Optional.of(module);
    }
  }

  /**
   * Add a USE statement of the module named moduleName in the current module.
   *
   * @param currentNode The current node as an Xnode.
   * @param moduleName The name of the module imported by the USE statement.
   * @param xcodeml The current context.
   */
  public static void addUse(Xnode currentNode, String moduleName,
                            XcodeProgram xcodeml) 
    throws IllegalTransformationException
  {
    Optional<Xnode> optModule = ModuleHelper.getModule(currentNode);
    if (optModule.isPresent() && optModule.get() instanceof FmoduleDefinition)
    {
      FmoduleDefinition module = (FmoduleDefinition) optModule.get();
      module.getDeclarationTable().insertUseDecl(xcodeml, moduleName);
    } else if (optModule.isPresent() && optModule.get() instanceof FfunctionDefinition) {
      FfunctionDefinition fctDef = (FfunctionDefinition) optModule.get();
      fctDef.getDeclarationTable().insertUseDecl(xcodeml, moduleName);
    } else {
      throw new IllegalTransformationException(
        "Impossible to find program, module, function or subroutine", currentNode.lineNo());
    }
  }

  public static void addUses(Xnode currentNode, Collection<String> moduleNames,
                             XcodeProgram xcodeml)
    throws IllegalTransformationException
  {
    for(String moduleName : moduleNames){
      addUse(currentNode, moduleName, xcodeml);
    }
  }
}
