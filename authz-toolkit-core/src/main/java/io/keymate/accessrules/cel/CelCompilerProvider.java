package io.keymate.accessrules.cel;

import dev.cel.common.CelAbstractSyntaxTree;
import dev.cel.common.CelValidationResult;
import dev.cel.common.types.MapType;
import dev.cel.common.types.SimpleType;
import dev.cel.compiler.CelCompiler;
import dev.cel.compiler.CelCompilerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public final class CelCompilerProvider {

    private static final Logger log = LoggerFactory.getLogger(CelCompilerProvider.class);

    private static final MapType MAP_STRING_DYN =
            MapType.create(SimpleType.STRING, SimpleType.DYN);

    private final CelCompiler compiler;

    public CelCompilerProvider() {
        this.compiler = CelCompilerFactory.standardCelCompilerBuilder()
                .addVar("agent", MAP_STRING_DYN)
                .addVar("actor", MAP_STRING_DYN)
                .addVar("tenant", MAP_STRING_DYN)
                .addVar("user", MAP_STRING_DYN)
                .addVar("resource", MAP_STRING_DYN)
                .addVar("risk", MAP_STRING_DYN)
                .addVar("request", MAP_STRING_DYN)
                .build();
    }

    public CelCompiler getCompiler() {
        return compiler;
    }

    public CelAbstractSyntaxTree compile(String expression) throws CelCompilationException {
        CelValidationResult result = compiler.compile(expression);
        if (result.hasError()) {
            log.warn("CEL compilation failed: {}", result.getErrorString());
            throw new CelCompilationException(
                    "CEL compilation failed for [" + expression + "]: "
                            + result.getErrorString());
        }
        try {
            return result.getAst();
        } catch (Exception e) {
            log.warn("CEL AST extraction failed for expression", e);
            throw new CelCompilationException(
                    "CEL AST extraction failed for [" + expression + "]: "
                            + e.getMessage(), e);
        }
    }

    public List<String> validate(String expression) {
        List<String> errors = new ArrayList<>();
        try {
            CelValidationResult result = compiler.compile(expression);
            if (result.hasError()) {
                errors.add(result.getErrorString());
            }
        } catch (Exception e) {
            errors.add("Unexpected error: " + e.getMessage());
        }
        return errors;
    }

    public static class CelCompilationException extends RuntimeException {

        public CelCompilationException(String message) {
            super(message);
        }

        public CelCompilationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
