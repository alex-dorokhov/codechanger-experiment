package de.greenrobot.codechanger

import org.eclipse.jdt.core.dom.*
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite
import org.eclipse.jface.text.Document
import org.eclipse.text.edits.TextEdit
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging

class JdtCodeGenerator {
    //TODO use logger instead of println
    Logger logger = Logging.getLogger(JdtCodeGenerator)

    JdtCodeGenerator() {
    }

    def update(File javaFile) {
        String source = javaFile.text
        def parser = ASTParser.newParser(AST.JLS3)
        parser.setKind(ASTParser.K_COMPILATION_UNIT)
        // We optionally want to have the results of the semantic analysis
        parser.setResolveBindings(true)
        parser.setSource(source.chars)
        CompilationUnit cu = (CompilationUnit) parser.createAST(null)

        // Get a rewriter for this tree, that allows for transformation of the tree
        ASTRewrite astRewrite = ASTRewrite.create(cu.getAST())
        // Record the modifications, so we can later apply them to source document
        cu.recordModifications()

        // Tree transformation goes here
        println "start trasform ${javaFile.name}"
        transform(cu, astRewrite)

        // Get the text transformations corresponding to the tree transformations
        Document document = new Document(source)
        TextEdit edits = astRewrite.rewriteAST(document, null)
        edits.apply(document)
        String newSource = document.get()
        if (newSource != source) {
            println "rewrite ${javaFile.name}"
            javaFile.write(newSource)
        } else {
            println "code is not changed for ${javaFile.name}"
        }
    }

    def transform(CompilationUnit compilationUnit, ASTRewrite astRewrite) {
        compilationUnit.accept(new GeneratorVisitor(astRewrite))
    }

    class GeneratorVisitor extends ASTVisitor {
        boolean isEntity
        ASTRewrite astRewrite
        List<FieldDeclaration> fields = []
        boolean hasConstructor

        GeneratorVisitor(ASTRewrite astRewrite) {
            this.astRewrite = astRewrite
        }

        @Override
        boolean visit(MarkerAnnotation node) {
            if (node.typeName.fullyQualifiedName == 'Entity') {
                println "found entity annotation"
                isEntity = true
            }
            return true
        }

        @Override
        boolean visit(FieldDeclaration node) {
            fields.add(node)
            return true
        }

        @Override
        boolean visit(MethodDeclaration node) {
            if (node.constructor) {
                hasConstructor = true
            }
            return true
        }

        @Override
        void endVisit(TypeDeclaration typeDeclaration) {
            if (isEntity) {
                if (!hasConstructor) {
                    println "adding new constructor"
                    def fieldDefs = fields.collect { "${it.type} ${it.fragments()[0]}" }.join(', ')
                    def newConstructor = astRewrite.createStringPlaceholder("""
public ${typeDeclaration.name.identifier}($fieldDefs) {
    // here the body comes
}
""".toString(), TypeDeclaration.METHOD_DECLARATION)
                    def listRewrite = astRewrite.getListRewrite(typeDeclaration, TypeDeclaration.BODY_DECLARATIONS_PROPERTY)
                    listRewrite.insertLast(newConstructor, null)
                } else {
                    println "entity already has a constructor"
                }
            }
        }
    }
}
