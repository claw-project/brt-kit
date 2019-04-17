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
import claw.wani.transformation.ClawTransformation;

import java.util.HashSet;
import java.util.List;
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

    public static ReplacePow defaultPow() {
        return new ReplacePow("exponentiation", "portable_pow");
    }

    public ReplacePow(String moduleName, String funcName) {
        super();
        usageModuleName = moduleName;
        powFunctionName = funcName;
    }

    @Override
    public boolean analyze(XcodeProgram xcodeml, Translator translator) {
        return true;
    }

    @Override
    public boolean canBeTransformedWith(XcodeProgram xcodeml, Transformation other) {
        return false;
    }

    @Override
    public void transform(
        XcodeProgram xcodeml,
        Translator translator,
        Transformation transformation
    ) throws IllegalTransformationException {
        Set<Xnode> modModules = new HashSet<>();
        // get the exponentiation operator (**) usage
        List<Xnode> fPowers = xcodeml.matchAll(Xcode.F_POWER_EXPR);
        // if there is at least one usage we try to replace it
        if (! fPowers.isEmpty()) {
            // get dummy function types in case we need to replace an
            // operator by a function call
            String fctType = addDummyFctType(xcodeml);
            for (Xnode fPow : fPowers) {
                Optional<Xnode> optModule = ModuleHelper.getModule(fPow);
                if (optModule.isPresent()) {
                    Xnode module = optModule.get();
                    replaceExponentiation(fPow, xcodeml, fctType);
                    if (!modModules.contains(module.element())) {
                        // add the use statement
                        modModules.add(module);
                        ModuleHelper.addUse(module, usageModuleName, xcodeml);
                    }
                } else {
                    throw new IllegalTransformationException(
                        "Impossible to find program, module, function or subroutine"
                    );
                }
            }
        }
    }

    /**
     * Return a dummy function type that is needed when adding a function call
     * that is part of and imported (USE) module.
     *
     * @param  xcodeml The current context.
     * @return The dummy function type as a String.
     */
    private String addDummyFctType(XcodeProgram xcodeml) {
        Xnode dummyFctType = new FfunctionType(xcodeml);
        dummyFctType.setAttribute(Xattr.RETURN_TYPE, Xname.TYPE_F_REAL);
        String fctType = xcodeml.getTypeTable().generateHash(FortranType.REAL);
        dummyFctType.setAttribute(Xattr.TYPE, fctType);
        xcodeml.getTypeTable().add(new FfunctionType(dummyFctType));
        return fctType;
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
    private void replaceExponentiation(
        Xnode fPow,
        XcodeProgram xcodeml,
        String fctType
    ) throws IllegalTransformationException {
        int nChildren = fPow.children().size();
        if (nChildren != 2)
            throw new IllegalTransformationException(
                    "Unexpected number of arguments: " + nChildren, fPow.lineNo()
            );

        Xnode functionCall = xcodeml.createFctCall(
            Xname.TYPE_F_REAL,
            powFunctionName,
            fctType
        );
        Xnode argument = functionCall.matchDescendant(Xcode.ARGUMENTS);
        argument.append(fPow.child(0), true);
        argument.append(fPow.child(1), true);
        fPow.insertAfter(functionCall);
        XnodeUtil.safeDelete(fPow);
    }
}
