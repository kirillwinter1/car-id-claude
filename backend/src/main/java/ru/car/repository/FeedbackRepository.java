package ru.car.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.BeanPropertySqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import ru.car.model.Feedback;
import ru.car.model.Notification;
import ru.car.util.KeyHolderUtils;

import java.sql.Types;
import java.time.LocalDateTime;

@Repository
@RequiredArgsConstructor
public class FeedbackRepository {
    private static final String INSERT_NEW = "INSERT INTO feedbacks (user_id, channel, email, text, created_date) VALUES (:userId, :channel, :email, :text, :createdDate)";

    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    public Feedback save(Feedback entity) {
        entity.setCreatedDate(LocalDateTime.now());
        SqlParameterSource paramSource = paramSource(entity);
        KeyHolder keyHolder = new GeneratedKeyHolder();
        namedParameterJdbcTemplate.update(INSERT_NEW, paramSource, keyHolder);
        entity.setId(KeyHolderUtils.getId(keyHolder));
        return entity;
    }

    private SqlParameterSource paramSource(Feedback entity) {
        BeanPropertySqlParameterSource paramSource = new BeanPropertySqlParameterSource(entity);
        paramSource.registerSqlType("channel", Types.VARCHAR);
        return paramSource;
    }
}
