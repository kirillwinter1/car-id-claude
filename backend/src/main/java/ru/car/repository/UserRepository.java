package ru.car.repository;


import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.namedparam.BeanPropertySqlParameterSource;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import ru.car.model.User;
import ru.car.util.KeyHolderUtils;

import java.sql.Types;
import java.time.LocalDateTime;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class UserRepository {
    private static final String INSERT_NEW_USER = "INSERT INTO users (phone_number, role, active) VALUES (:phoneNumber, :role, :active)";
    private static final String UPDATE_USER = "UPDATE users set phone_number = :phoneNumber, role = :role, active = :active where id = :id";
    private static final String SELECT_USER_BY_PHONE = "SELECT u.* FROM users u where u.phone_number = :phoneNumber";
    private static final String SELECT_USER_BY_ID = "SELECT u.* FROM users u where u.id = :id";

    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    public User save(User entity) {
        entity.setCreateDate(LocalDateTime.now());
        SqlParameterSource paramSource = paramSource(entity);
        KeyHolder keyHolder = new GeneratedKeyHolder();
        namedParameterJdbcTemplate.update(INSERT_NEW_USER, paramSource, keyHolder);
        entity.setId(KeyHolderUtils.getId(keyHolder));
        return entity;
    }

    public User update(User entity) {
        SqlParameterSource paramSource = paramSource(entity);
        namedParameterJdbcTemplate.update(UPDATE_USER, paramSource);
        return entity;
    }

    public Optional<User> findByPhoneNumber(String phoneNumber) {
        if (StringUtils.isEmpty(phoneNumber)) {
            return Optional.empty();
        }
        MapSqlParameterSource paramSource = new MapSqlParameterSource("phoneNumber", phoneNumber);
        return findOptionalUser(SELECT_USER_BY_PHONE, paramSource);
    }

    public Optional<User> findById(Long id) {
        MapSqlParameterSource paramSource = new MapSqlParameterSource("id", id);
        return findOptionalUser(SELECT_USER_BY_ID, paramSource);
    }

    private Optional<User> findOptionalUser(String sql, SqlParameterSource paramSource) {
        try {
            return Optional.ofNullable(namedParameterJdbcTemplate.queryForObject(
                    sql,
                    paramSource,
                    BeanPropertyRowMapper.newInstance(User.class)
            ));
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    private SqlParameterSource paramSource(User entity) {
        BeanPropertySqlParameterSource paramSource = new BeanPropertySqlParameterSource(entity);
        paramSource.registerSqlType("role", Types.VARCHAR);
        return paramSource;
    }
}

