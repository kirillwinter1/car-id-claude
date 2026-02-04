package ru.car.repository;

import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.BooleanUtils;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.namedparam.BeanPropertySqlParameterSource;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import ru.car.model.AuthenticationCode;
import ru.car.util.KeyHolderUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class AuthenticationCodeRepository {
    private static final String INSERT_NEW_AUTHENTICATION_CODE = "INSERT INTO authentication_code (phone_number, code, created_date) VALUES (:phoneNumber, :code, :createdDate)";
    private static final String UPDATE_BY_PHONE = "UPDATE authentication_code SET code = :code, created_date = :createdDate WHERE phone_number = :phoneNumber";;
    private static final String SELECT_BY_PHONE = "SELECT a.* FROM authentication_code a where a.phone_number = :phoneNumber";
    private static final String DELETE_BY_PHONE = "DELETE FROM authentication_code a WHERE a.phone_number = :phoneNumber";
    private static final String EXISTS_BY_PHONE_AND_CODE = "SELECT EXISTS(SELECT 1 FROM authentication_code a WHERE a.phone_number = :phoneNumber and a.code = :code)";
    private static final String EXISTS_BY_PHONE_AND_AFTER = "SELECT EXISTS(SELECT 1 FROM authentication_code a WHERE a.phone_number = :phoneNumber and a.created_date >= :createdDate)";

    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    public AuthenticationCode save(AuthenticationCode entity) {
        entity.setCreatedDate(LocalDateTime.now());
        BeanPropertySqlParameterSource paramSource = new BeanPropertySqlParameterSource(entity);
        KeyHolder keyHolder = new GeneratedKeyHolder();
        namedParameterJdbcTemplate.update(INSERT_NEW_AUTHENTICATION_CODE, paramSource, keyHolder);
        entity.setId(KeyHolderUtils.getId(keyHolder));
        return entity;
    }

    public AuthenticationCode updateAuthenticationCodeCodeByTelephone(AuthenticationCode entity) {
        entity.setCreatedDate(LocalDateTime.now());
        BeanPropertySqlParameterSource paramSource = new BeanPropertySqlParameterSource(entity);
        namedParameterJdbcTemplate.update(UPDATE_BY_PHONE, paramSource);
        return entity;
    }

    public List<AuthenticationCode> findByAllPhoneNumber(String telephone) {
        MapSqlParameterSource paramSource = new MapSqlParameterSource("phoneNumber", telephone);
        return findAllAuthenticationCode(SELECT_BY_PHONE, paramSource);
    }

    public boolean deleteAllByTelephone(String telephone) {
        MapSqlParameterSource paramSource = new MapSqlParameterSource("phoneNumber", telephone);
        return namedParameterJdbcTemplate.update(DELETE_BY_PHONE, paramSource) == 1;
    }

    public boolean existsByPhoneNumberAndAfter(String telephone, LocalDateTime date) {
        MapSqlParameterSource paramSource = new MapSqlParameterSource("phoneNumber", telephone)
                .addValue("createdDate", date);
        return BooleanUtils.isTrue(
                namedParameterJdbcTemplate.queryForObject(EXISTS_BY_PHONE_AND_AFTER, paramSource, Boolean.class));
    }

    public boolean existByTelephoneAndCode(String telephone, String code) {
        MapSqlParameterSource paramSource = new MapSqlParameterSource("phoneNumber", telephone)
                .addValue("code", code);
        return BooleanUtils.isTrue(
                namedParameterJdbcTemplate.queryForObject(EXISTS_BY_PHONE_AND_CODE, paramSource, Boolean.class));
    }

    private List<AuthenticationCode> findAllAuthenticationCode(String sql, SqlParameterSource paramSource) {
        try {
            return namedParameterJdbcTemplate.query(
                    sql,
                    paramSource,
                    BeanPropertyRowMapper.newInstance(AuthenticationCode.class)
            );
        } catch (EmptyResultDataAccessException e) {
            return new ArrayList<>();
        }
    }
}
