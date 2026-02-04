package ru.car.mapper;

import org.mapstruct.Mapper;
import ru.car.dto.UserDto;
import ru.car.model.User;

@Mapper(componentModel = "spring")
public interface UserDtoMapper extends DtoMapper<User, UserDto>  {
    UserDto toDto(User user);
    User toEntity(UserDto userDto);
}
