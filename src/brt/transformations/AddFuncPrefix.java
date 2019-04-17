package brt.transformations;

import claw.shenron.transformation.Transformation;
import claw.shenron.translator.Translator;
import claw.tatsu.xcodeml.exception.IllegalTransformationException;
import claw.tatsu.xcodeml.xnode.common.Xcode;
import claw.tatsu.xcodeml.xnode.common.XcodeProgram;
import claw.tatsu.xcodeml.xnode.common.Xnode;
import claw.wani.transformation.ClawTransformation;
import claw.wani.x2t.configuration.Configuration;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Add import statement (USE) when given transcendental functions are used.
 * This is for shadowing purpose. The modules to import and functions to shadow
 * are listed in a map declared as a class field. Hence for now this information
 * is hardcoded.
 */
public class AddFuncPrefix extends ClawTransformation {
  private final String prefix;
  private final Set<String> functions;
  private final Set<String> modules;

  public AddFuncPrefix() {
    super();
    this.prefix = Configuration.get().getParameter("br_function_prefix");
    this.functions = new HashSet<>();
    functions.add("sin");
    this.modules = new HashSet<>();
    modules.add("mo_br");
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
    Stream<Xnode> modProgFuncSub =
      xcodeml.matchAll(Xcode.FUNCTION_CALL).stream().filter(
        x -> functions.contains(x.matchDescendant(Xcode.NAME).value().toLowerCase())
      ).map(y -> {
        Xnode nameName = y.matchDescendant(Xcode.NAME);
        nameName.setValue(prefix + nameName.value());
        return ModuleHelper.getModule(y);
      })
      .filter(o -> o.isPresent())
      .map(o -> o.get());

      modProgFuncSub.collect(Collectors.toSet())
        .forEach(x -> ModuleHelper.addUses(x, modules, xcodeml));
  }
}
