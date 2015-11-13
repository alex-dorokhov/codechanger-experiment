package de.greenrobot.codechanger

import com.github.javaparser.ASTHelper
import com.github.javaparser.JavaParser
import com.github.javaparser.ast.CompilationUnit
import com.github.javaparser.ast.body.AnnotationDeclaration
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration
import com.github.javaparser.ast.body.ConstructorDeclaration
import com.github.javaparser.ast.body.FieldDeclaration
import com.github.javaparser.ast.stmt.BlockStmt
import com.github.javaparser.ast.visitor.VoidVisitorAdapter
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging

import java.lang.reflect.Modifier

class CodeGenerator {
    Logger logger = Logging.getLogger(CodeGenerator)

    CodeGenerator() {
    }

    def update(File javaFile) {
        CompilationUnit cu = JavaParser.parse(javaFile)
        logger.info("Checking $javaFile")
        def visitor = new GeneratorVisitor()
        visitor.visit(cu, null)
        if (visitor.changed) {
            javaFile.write(cu.toString())
        }
    }

    class GeneratorVisitor extends VoidVisitorAdapter {
        boolean changed
        List<FieldDeclaration> fields = []
        List<ConstructorDeclaration> constructors = []

        @Override
        void visit(ClassOrInterfaceDeclaration n, Object arg) {
            if (n.annotations.find { it.name.name == 'Entity' }) {
                logger.debug("Found entity class ${n.name}")
                super.visit(n, arg)

                if (!constructors) {
                    logger.info("Adding constructor to ${n.name}")
                    def constructor = new ConstructorDeclaration()
                    constructor.parameters = fields.collect {
                        ASTHelper.createParameter(it.type, it.variables[0].id.name)
                    }
                    constructor.name = n.name
                    constructor.block = new BlockStmt([])
                    constructor.modifiers = Modifier.PUBLIC
                    ASTHelper.addMember(n, constructor)

                    changed = true
                }
            }
        }

        @Override
        void visit(FieldDeclaration n, Object arg) {
            super.visit(n, arg)
            fields << n
        }

        @Override
        void visit(ConstructorDeclaration n, Object arg) {
            super.visit(n, arg)
            constructors << n
        }
    }
}
