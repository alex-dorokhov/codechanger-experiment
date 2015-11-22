package de.greenrobot.codechanger

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.SourceSetContainer

class JdtCodeChangerPlugin implements Plugin<Project> {
    @Override
    void apply(Project project) {
        project.with {
             task('generateCode') << {
                SourceSetContainer sourceSets = project.sourceSets
                def classes = sourceSets*.allJava*.files.flatten()
                def generator = new JdtCodeGenerator()
                classes.each { generator.update(it) }
            }

            compileJava.dependsOn generateCode
        }
    }
}
