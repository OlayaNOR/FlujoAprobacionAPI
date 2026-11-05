package com.nicolas.flujos.arquitectura;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

// Los módulos de Maven ya impiden casi todo esto al compilar. Estas reglas cubren lo que Maven no ve:
// qué partes de Spring entran a la aplicación y que nadie se salte los casos de uso.
// Está en flujos-api porque es el único módulo que tiene todas las clases en el classpath.
@AnalyzeClasses(packages = "com.nicolas.flujos", importOptions = ImportOption.DoNotIncludeTests.class)
class ArquitecturaTest {

    @ArchTest
    static final ArchRule dominioSinFrameworks =
        classes().that().resideInAPackage("..domain..")
            .should().onlyDependOnClassesThat().resideInAnyPackage("java..", "..domain..");

    // Spring entra a la aplicación solo para declarar beans y transacciones
    @ArchTest
    static final ArchRule aplicacionSoloConSpringBasico =
        classes().that().resideInAPackage("..application..")
            .should().onlyDependOnClassesThat().resideInAnyPackage(
                "java..", "..domain..", "..application..",
                "org.springframework.stereotype..",
                "org.springframework.beans.factory.annotation..",
                "org.springframework.transaction..");

    @ArchTest
    static final ArchRule capas =
        layeredArchitecture().consideringOnlyDependenciesInLayers()
            .layer("Dominio").definedBy("..domain..")
            .layer("Aplicacion").definedBy("..application..")
            .layer("Infraestructura").definedBy("..infrastructure..")
            .layer("Api").definedBy("..api..")
            .whereLayer("Api").mayNotBeAccessedByAnyLayer()
            .whereLayer("Infraestructura").mayNotBeAccessedByAnyLayer()
            .whereLayer("Aplicacion").mayOnlyBeAccessedByLayers("Infraestructura", "Api");

    @ArchTest
    static final ArchRule controladoresSoloConCasosDeUso =
        noClasses().that().resideInAPackage("..api..")
            .should().dependOnClassesThat().resideInAnyPackage("..application.port.out..", "..application.service..");

    // Si una implementación fuera pública, otra clase podría usarla directamente y saltarse la interfaz
    @ArchTest
    static final ArchRule serviciosNoPublicos =
        classes().that().resideInAPackage("..application.service..")
            .should().notBePublic();
}
