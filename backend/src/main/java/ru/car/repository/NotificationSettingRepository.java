package ru.car.repository;

import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.BooleanUtils;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.namedparam.BeanPropertySqlParameterSource;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import ru.car.model.NotificationSetting;
import ru.car.util.KeyHolderUtils;

import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class NotificationSettingRepository {
    private static final String SELECT_BY_USER_ID = "SELECT n.* FROM notification_settings n WHERE n.user_id = :userId";
    private static final String EXISTS_BY_TELEGRAM_DIALOG_ID = "SELECT EXISTS( SELECT 1 FROM notification_settings n WHERE n.telegram_dialog_id = :telegramDialogId )";
    private static final String SELECT_BY_TELEGRAM_DIALOG_ID = "SELECT n.user_id FROM notification_settings n WHERE n.telegram_dialog_id = :telegramDialogId";
    private static final String SELECT_BY_QR_ID = "SELECT n.* FROM notification_settings n left join qrs q on n.user_id = q.user_id WHERE q.id = :qrId";
    private static final String INSERT_NEW = "INSERT INTO notification_settings (user_id, push_enabled, call_enabled, telegram_enabled, whatsapp_enabled, active) VALUES (:userId, :pushEnabled, :callEnabled, :telegramEnabled, :whatsappEnabled, :active)";
    private static final String UPDATE_ALL_BY_USER_ID = "UPDATE notification_settings SET push_enabled = :pushEnabled, call_enabled = :callEnabled, telegram_enabled = :telegramEnabled, whatsapp_enabled = :whatsappEnabled, active = :active, telegram_dialog_id = :telegramDialogId WHERE user_id = :userId";
    private static final String UPDATE_TELEGRAM_DIALOG_ID_BY_USER_ID = "UPDATE notification_settings SET telegram_enabled = true, telegram_dialog_id = :telegramDialogId WHERE user_id = :userId";


    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    public NotificationSetting findByUserId(Long id) {
        MapSqlParameterSource paramSource = new MapSqlParameterSource("userId", id);
        return namedParameterJdbcTemplate.queryForObject(SELECT_BY_USER_ID, paramSource, BeanPropertyRowMapper.newInstance(NotificationSetting.class));
    }

    public NotificationSetting save(NotificationSetting entity) {
        BeanPropertySqlParameterSource paramSource = new BeanPropertySqlParameterSource(entity);
        KeyHolder keyHolder = new GeneratedKeyHolder();
        namedParameterJdbcTemplate.update(INSERT_NEW, paramSource, keyHolder);
        entity.setId(KeyHolderUtils.getId(keyHolder));
        return entity;
    }

    public NotificationSetting update(NotificationSetting entity) {
        BeanPropertySqlParameterSource paramSource = new BeanPropertySqlParameterSource(entity);
        namedParameterJdbcTemplate.update(UPDATE_ALL_BY_USER_ID, paramSource);
        return entity;
    }

    public boolean updateTelegramDialogIdByUserId(Long userId, Long telegramDialogId) {
        MapSqlParameterSource paramSource = new MapSqlParameterSource("userId", userId)
                .addValue("telegramDialogId", telegramDialogId);
        return 1 == namedParameterJdbcTemplate.update(UPDATE_TELEGRAM_DIALOG_ID_BY_USER_ID, paramSource);
    }

    public NotificationSetting findByQrId(UUID qrId) {
        MapSqlParameterSource paramSource = new MapSqlParameterSource("qrId", qrId);
        return namedParameterJdbcTemplate.queryForObject(SELECT_BY_QR_ID, paramSource, BeanPropertyRowMapper.newInstance(NotificationSetting.class));
    }

    public Boolean existsByTelegramDialogId(long chatId) {
        MapSqlParameterSource paramSource = new MapSqlParameterSource("telegramDialogId", chatId);
        return BooleanUtils.isTrue(
                namedParameterJdbcTemplate.queryForObject(EXISTS_BY_TELEGRAM_DIALOG_ID, paramSource, Boolean.class));
    }

    public Long findUserIdByTelegramDialogId(long chatId) {
        MapSqlParameterSource paramSource = new MapSqlParameterSource("telegramDialogId", chatId);
        return namedParameterJdbcTemplate.queryForObject(SELECT_BY_TELEGRAM_DIALOG_ID, paramSource, Long.class);
    }
}
