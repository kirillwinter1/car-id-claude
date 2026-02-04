package ru.car.repository;

import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.BooleanUtils;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.namedparam.BeanPropertySqlParameterSource;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.stereotype.Repository;
import ru.car.model.FirebaseToken;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class FirebaseTokenRepository {
    private static final String SELECT_BY_AUTH_ID = "SELECT f.* FROM firebase_tokens f WHERE f.auth_id = :authId";
    private static final String SELECT_BY_TOKEN_AND_USER_ID = "SELECT f.* FROM firebase_tokens f WHERE f.token = :token and f.user_id = :userId";
    private static final String UPDATE_TOKEN_BY_ID = "UPDATE firebase_tokens SET token = :token, auth_id = :authId, created_date = :createdDate WHERE id = :id";
    private static final String INSERT_NEW = "INSERT INTO firebase_tokens (token, user_id, auth_id, created_date) VALUES (:token, :userId, :authId, :createdDate)";
    private static final String EXISTS_BY_USER_ID = "SELECT EXISTS(SELECT 1 FROM firebase_tokens f WHERE f.user_id = :userId)";
    private static final String SELECT_BY_USER_ID = "SELECT f.* FROM firebase_tokens f WHERE f.user_id = :userId";

    private static final String DELETE_BY_USER_ID = "DELETE FROM firebase_tokens WHERE user_id = :userId";
    private static final String DELETE_BY_AUTH_ID = "DELETE FROM firebase_tokens WHERE auth_id = :authId";


    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    public Optional<FirebaseToken> findByAuthId(Long authId) {
        MapSqlParameterSource paramSource = new MapSqlParameterSource("authId", authId);
        return findOptional(SELECT_BY_AUTH_ID, paramSource);
    }

    public Optional<FirebaseToken> findByTokenAndUserId(String token, Long userId) {
        MapSqlParameterSource paramSource = new MapSqlParameterSource("token", token)
                .addValue("userId", userId);
        return findOptional(SELECT_BY_TOKEN_AND_USER_ID, paramSource);
    }

    public FirebaseToken updateTokenById(FirebaseToken entity) {
        entity.setCreatedDate(LocalDateTime.now());
        BeanPropertySqlParameterSource paramSource = new BeanPropertySqlParameterSource(entity);
        namedParameterJdbcTemplate.update(UPDATE_TOKEN_BY_ID, paramSource);
        return entity;
    }

    public FirebaseToken save(FirebaseToken entity) {
        entity.setCreatedDate(LocalDateTime.now());
        BeanPropertySqlParameterSource paramSource = new BeanPropertySqlParameterSource(entity);
        namedParameterJdbcTemplate.update(INSERT_NEW, paramSource);
        return entity;
    }

    private Optional<FirebaseToken> findOptional(String sql, SqlParameterSource paramSource) {
        try {
            return Optional.ofNullable(namedParameterJdbcTemplate.queryForObject(
                    sql,
                    paramSource,
                    BeanPropertyRowMapper.newInstance(FirebaseToken.class)
            ));
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    public boolean existsByUserId(Long userId) {
        MapSqlParameterSource paramSource = new MapSqlParameterSource("userId", userId);
        return BooleanUtils.isTrue(
                namedParameterJdbcTemplate.queryForObject(EXISTS_BY_USER_ID, paramSource, Boolean.class));
    }

    public List<FirebaseToken> findAllByUserId(Long userId) {
        MapSqlParameterSource paramSource = new MapSqlParameterSource("userId", userId);
        return namedParameterJdbcTemplate.query(SELECT_BY_USER_ID, paramSource, BeanPropertyRowMapper.newInstance(FirebaseToken.class));
    }

    public void deleteAllByUserId(Long userId) {
        MapSqlParameterSource paramSource = new MapSqlParameterSource("userId", userId);
        namedParameterJdbcTemplate.update(DELETE_BY_USER_ID, paramSource);
    }

    public void deleteByAuthId(Long authId) {
        MapSqlParameterSource paramSource = new MapSqlParameterSource("authId", authId);
        namedParameterJdbcTemplate.update(DELETE_BY_AUTH_ID, paramSource);
    }
}
