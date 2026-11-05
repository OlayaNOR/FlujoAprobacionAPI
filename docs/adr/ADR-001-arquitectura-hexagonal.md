# ADR-001: Arquitectura hexagonal con módulos Maven

## Contexto

El dominio (estados, invariantes, reglas de aprobación) es la parte que más va a cambiar y la que más necesita pruebas rápidas. Si depende de Spring o JPA, esas pruebas se vuelven lentas y las entidades terminan con setters y constructores vacíos.

## Decisión

Separar el código en cuatro módulos Maven: `flujos-domain`, `flujos-app`, `flujos-infra` y `flujos-api`. Las dependencias entre capas las hace cumplir el compilador. `flujos-api` recibe a `flujos-infra` con scope `runtime`, así un controlador no puede importar un repositorio JPA por accidente.

`flujos-domain` no tiene ninguna dependencia de compilación. `flujos-app` solo va a usar `spring-context` y `spring-tx`, para `@Service` y `@Transactional`. Sacar esas dos anotaciones exigiría una capa extra de configuración que no aporta nada a este tamaño de proyecto.

## Consecuencias

- Más archivos `pom.xml` y un build algo más largo.
- Las entidades JPA son clases distintas de los agregados, con un mapper entre ambas.
- Los módulos no cubren todo: no impiden que `flujos-app` use otras partes de Spring, ni que un controlador llame a un servicio sin pasar por su interfaz. Eso lo revisan las pruebas de ArchUnit en `flujos-api`, que es el único módulo con todas las clases en el classpath.
