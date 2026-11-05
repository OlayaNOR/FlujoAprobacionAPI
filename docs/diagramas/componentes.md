# Componentes

```mermaid
flowchart LR
    cliente([Cliente / Swagger / Bruno])

    subgraph api [flujos-api]
        controladores[Controladores y DTOs]
        errores[Manejador de errores RFC 7807]
    end

    subgraph app [flujos-app]
        portin[[port.in: casos de uso]]
        servicios[Servicios]
        portout[[port.out: repositorios, eventos, métricas]]
    end

    subgraph dominio [flujos-domain]
        solicitud[Solicitud]
        flujo[Flujo]
        auditoria[RegistroAuditoria]
    end

    subgraph infra [flujos-infra]
        seguridad[Filtros JWT y límite de login]
        jpa[Adaptadores JPA]
        jobs[Relevo del outbox y escalamiento]
        metricas[Micrometer]
    end

    db[(PostgreSQL)]

    cliente --> seguridad --> controladores
    controladores --> portin
    servicios -. implementa .-> portin
    servicios --> portout
    servicios --> dominio
    jpa -. implementa .-> portout
    metricas -. implementa .-> portout
    jobs --> portin
    jpa --> db
    jobs --> db
```

Las flechas de dependencia apuntan hacia el dominio. `flujos-api` solo ve `flujos-infra` en tiempo de ejecución, así que un controlador no puede importar un repositorio aunque quiera; lo que Maven no alcanza a impedir lo revisan las pruebas de ArchUnit ([ADR-001](../adr/ADR-001-arquitectura-hexagonal.md)).
