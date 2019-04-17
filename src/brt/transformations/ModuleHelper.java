package brt.transformations;

import claw.tatsu.xcodeml.xnode.common.Xattr;
import claw.tatsu.xcodeml.xnode.common.Xcode;
import claw.tatsu.xcodeml.xnode.common.XcodeProgram;
import claw.tatsu.xcodeml.xnode.common.Xnode;

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
            Xnode pfs = node.matchAncestor(Xcode.F_FUNCTION_DEFINITION);
            // if null then there is no PROGRAM or MODULE
            if (pfs == null) return Optional.empty();
            else return Optional.of(pfs);
        }
        else return Optional.of(module);
    }

    /**
     * Add a USE statement of the module named moduleName in the current module.
     *
     * @param currentModule The current module as an Xnode.
     * @param moduleName The name of the module imported by the USE statement.
     * @param xcodeml The current context.
     */
    public static void addUse(
        Xnode currentModule,
        String moduleName,
        XcodeProgram xcodeml
    ) {
        Xnode use = xcodeml.createNode(Xcode.F_USE_DECL);
        use.setAttribute(Xattr.NAME, moduleName);
        currentModule.matchDirectDescendant(Xcode.DECLARATIONS).insert(use, false);
    }

    public static void addUses(
        Xnode currentModule,
        Collection<String> moduleName,
        XcodeProgram xcodeml
    ) {
        moduleName.forEach(name -> addUse(currentModule, name, xcodeml));
    }
}
