package ru.car.mapper;

import java.util.Collection;
import java.util.List;

public interface DtoMapper<Entity, Dto> {
    Dto toDto(Entity entity);
    Entity toEntity(Dto dto);
    List<Dto> toDto(Collection<Entity> entity);
    List<Entity> toEntity(Collection<Dto> dto);
}
