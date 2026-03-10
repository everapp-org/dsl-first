package org.everapp.generator;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.TypeSpec;
import javax.lang.model.element.Modifier;
import java.io.File;
import java.util.stream.Collectors;

// Ensure you run `mvn antlr4:antlr4` first to generate the parser classes
import org.everapp.MyDSLLexer;
import org.everapp.MyDSLParser;
import org.everapp.MyDSLParser.DomainContext;

public class MainGenerator {
    public static void main(String[] args) throws Exception {
        // Read the DSL file
        CharStream input = CharStreams.fromFileName("src/main/resources/domain.dsl");
        MyDSLLexer lexer = new MyDSLLexer(input);
        MyDSLParser parser = new MyDSLParser(new CommonTokenStream(lexer));
        
        // Parse AST
        DomainContext domain = parser.domain();
        String domainName = domain.ID().getText();
        
        // Build an Enum for the states using JavaPoet
        TypeSpec.Builder enumBuilder = TypeSpec.enumBuilder(domainName + "State")
            .addModifiers(Modifier.PUBLIC);
            
        domain.states().ID().forEach(idNode -> {
            enumBuilder.addEnumConstant(idNode.getText());
        });
        
        TypeSpec stateEnum = enumBuilder.build();
        
        // Write the java file out
        JavaFile javaFile = JavaFile.builder("org.everapp.generated", stateEnum)
            .build();
            
        File outputDir = new File("target/generated-sources/dsl");
        outputDir.mkdirs();
        javaFile.writeTo(outputDir);
        
        System.out.println("✅ Generated " + domainName + "State.java successfully in " + outputDir.getAbsolutePath());
    }
}
