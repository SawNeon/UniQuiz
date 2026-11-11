package br.com.unimed.uniquiz;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

@AnalyzeClasses(packages = "br.com.unimed.uniquiz", importOptions = ImportOption.DoNotIncludeTests.class)
class ArquiteturaTest {

    @ArchTest
    static final ArchRule dominioNaoDependeDeFramework = noClasses()
            .that()
            .resideInAPackage("..dominio..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage("org.springframework..", "jakarta.persistence..", "com.fasterxml..");

    @ArchTest
    static final ArchRule dominioNaoDependeDasOutrasCamadas = noClasses()
            .that()
            .resideInAPackage("..dominio..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage("..aplicacao..", "..persistencia..", "..web..");

    @ArchTest
    static final ArchRule ninguemDependeDaCamadaWeb = noClasses()
            .that()
            .resideOutsideOfPackage("..web..")
            .should()
            .dependOnClassesThat()
            .resideInAPackage("..web..");
}
