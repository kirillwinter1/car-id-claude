package ru.car.repository;


import lombok.RequiredArgsConstructor;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.BeanPropertySqlParameterSource;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.stereotype.Repository;
import ru.car.dto.PageParam;
import ru.car.enums.NotificationStatus;
import ru.car.model.Notification;
import ru.car.rowMapper.NotificationRowMapper;

import java.sql.Types;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class NotificationRepository {
    private static final String INSERT_NEW = "INSERT INTO notifications (id, qr_id, reason_id, text, created_date, status, sender_id, visitor_id) VALUES (:id, :qrId, :reasonId, :text, :createdDate, :status, :senderId, :visitorId)";
    private static final String SELECT_BY_ID = "SELECT n.* , " +
            " qr.id as \"qr.id\", " +
            " qr.batch_id as \"qr.batchId\", " +
            " qr.seq_number as \"qr.seqNumber\", " +
            " qr.name as \"qr.name\", " +
            " qr.printed as \"qr.printed\", " +
            " qr.status as \"qr.status\", " +
            " qr.created_date as \"qr.createdDate\", " +
            " qr.user_id as \"qr.userId\", " +
            " rd.id as \"rd.id\", " +
            " rd.description as \"rd.description\" " +
            " FROM notifications as n " +
            " join qrs qr on qr.id = n.qr_id " +
            " join reason_dictionary rd on rd.id = n.reason_id " +
            " WHERE n.id = :id";

    private static final String SELECT_BY_CALL_ID = "SELECT n.* " +
            " FROM notifications as n " +
            " WHERE n.call_id = :callId";

    private static final String SELECT_COUNT_BY_USER_ID_AND_STATUS = "SELECT count(*) FROM notifications n left join qrs q on n.qr_id = q.id WHERE q.user_id = :userId and n.status = :status";
    private static final String SELECT_COUNT_BY_SENDER_ID_AND_STATUS = "SELECT count(*) FROM notifications n WHERE n.sender_id = :senderId and n.status = :status";
    private static final String SELECT_COUNT_BY_USER_ID = "SELECT count(*) " +
            " FROM notifications n left join qrs q on n.qr_id = q.id " +
            " WHERE q.user_id = :userId and n.status != :draft";
    private static final String SELECT_COUNT_BY_SENDER_ID = "SELECT count(*) " +
            " FROM notifications n " +
            " WHERE n.sender_id = :senderId and n.status != :draft";
    private static final String SELECT_ALL_BY_PAGE_AND_USER_ID = "SELECT n.* , " +
            " qr.id as \"qr.id\", " +
            " qr.batch_id as \"qr.batchId\", " +
            " qr.seq_number as \"qr.seqNumber\", " +
            " qr.name as \"qr.name\", " +
            " qr.printed as \"qr.printed\", " +
            " qr.status as \"qr.status\", " +
            " qr.created_date as \"qr.createdDate\", " +
            " qr.user_id as \"qr.userId\", " +
            " rd.id as \"rd.id\", " +
            " rd.description as \"rd.description\" " +
            " FROM notifications as n " +
            " join qrs qr on n.qr_id = qr.id " +
            " join reason_dictionary rd on rd.id = n.reason_id " +
            " WHERE qr.user_id = :userId and n.status != :draft " +
            " ORDER BY n.status DESC, n.created_date DESC " +
            " OFFSET :offset ROWS FETCH NEXT :page_size ROWS ONLY";

    private static final String SELECT_ALL_BY_PAGE_AND_SENDER_ID = "SELECT n.* , " +
            " rd.id as \"rd.id\", " +
            " rd.description as \"rd.description\" " +
            " FROM notifications as n " +
            " join reason_dictionary rd on rd.id = n.reason_id " +
            " WHERE n.sender_id = :senderId and n.status != :draft " +
            " ORDER BY n.status DESC, n.created_date DESC " +
            " OFFSET :offset ROWS FETCH NEXT :page_size ROWS ONLY";

    private static final String SELECT_ALL_BY_STATUS_AND_USER_ID = "SELECT n.* , " +
            " qr.id as \"qr.id\", " +
            " qr.batch_id as \"qr.batchId\", " +
            " qr.seq_number as \"qr.seqNumber\", " +
            " qr.name as \"qr.name\", " +
            " qr.printed as \"qr.printed\", " +
            " qr.status as \"qr.status\", " +
            " qr.created_date as \"qr.createdDate\", " +
            " qr.user_id as \"qr.userId\", " +
            " rd.id as \"rd.id\", " +
            " rd.description as \"rd.description\" " +
            " FROM notifications as n " +
            " join qrs qr on n.qr_id = qr.id " +
            " join reason_dictionary rd on rd.id = n.reason_id " +
            " WHERE qr.user_id = :userId and n.status = :status " +
            " ORDER BY n.created_date DESC " +
            " OFFSET :offset ROWS FETCH NEXT :page_size ROWS ONLY";

    private static final String SELECT_ALL_BY_STATUS_AND_SENDER_ID = "SELECT n.* , " +
            " rd.id as \"rd.id\", " +
            " rd.description as \"rd.description\" " +
            " FROM notifications as n " +
            " join reason_dictionary rd on rd.id = n.reason_id " +
            " WHERE n.sender_id = :senderId and n.status = :status " +
            " ORDER BY n.created_date DESC " +
            " OFFSET :offset ROWS FETCH NEXT :page_size ROWS ONLY";
    private static final String SELECT_BY_QR_ID_AND_CREATED_DATE = "SELECT n.* FROM notifications n WHERE n.qr_id = :qrId and n.created_date >= :createdDate";
    private static final String SELECT_COUNT_BY_VISITOR_ID_AND_CREATED_DATE = "SELECT count(n.*) FROM notifications n WHERE n.visitor_id = :visitorId and n.created_date >= :createdDate";
    private static final String UPDATE_STATUS_BY_ID = "UPDATE notifications SET status = :status WHERE id = :id";
    private static final String UPDATE_CALL_ID_BY_ID = "UPDATE notifications SET call_id = :callId WHERE id = :id";
    private static final String UPDATE_REASON_AND_TEXT_AND_STATUS_BY_ID = "UPDATE notifications SET reason_id = :reasonId, text = :text, status = :status WHERE id = :id";
    private static final String DELETE_BY_ID = "DELETE FROM notifications n WHERE n.id = :id";
    private static final String DELETE_BY_QR_ID = "DELETE FROM notifications n WHERE n.qr_id = :qrId";
    private static final String DELETE_BY_USER_ID = "DELETE FROM notifications WHERE id in (" +
            " SELECT n.id FROM notifications n join qrs qr on n.qr_id = qr.id WHERE qr.user_id = :userId )";


    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

//    public List<Notification> findAll() {
//        return jdbcTemplate.query(
//                "SELECT * FROM notifications n left join users u ON n.user_id = u.id left join reason_dictionary rd ON n.reason_id = rd.id",
//                new NotificationRowMapper()
//        );
//    }

    public Optional<Notification> findById(UUID id) {
        MapSqlParameterSource paramSource = new MapSqlParameterSource("id", id);
        return findOptionalNotification(SELECT_BY_ID, paramSource);
    }

    public Optional<Notification> findByCallId(Long callId) {
        MapSqlParameterSource paramSource = new MapSqlParameterSource("callId", callId);
        return findOptionalNotification(SELECT_BY_CALL_ID, paramSource);
    }

    public Notification save(Notification entity) {
        entity.setId(UUID.randomUUID());
        entity.setCreatedDate(LocalDateTime.now());
        SqlParameterSource paramSource = paramSource(entity);
        namedParameterJdbcTemplate.update(INSERT_NEW, paramSource);
        return entity;
    }

    public Notification updateStatus(Notification entity) {
        SqlParameterSource paramSource = paramSource(entity);
        namedParameterJdbcTemplate.update(UPDATE_STATUS_BY_ID, paramSource);
        return entity;
    }

    public boolean updateCallId(UUID id, Long callId) {
        MapSqlParameterSource paramSource = new MapSqlParameterSource("id", id)
                .addValue("callId", callId);
        return namedParameterJdbcTemplate.update(UPDATE_CALL_ID_BY_ID, paramSource) == 1;
    }

    public Notification updateReasonAndTextAndStatus(Notification entity) {
        SqlParameterSource paramSource = paramSource(entity);
        namedParameterJdbcTemplate.update(UPDATE_REASON_AND_TEXT_AND_STATUS_BY_ID, paramSource);
        return entity;
    }

//    public Optional<Notification> findByQrId(UUID id) {
//        String sql = "SELECT * FROM notifications n " +
//                "left join users u ON n.user_id = u.id " +
//                "left join reason_dictionary rd ON n.reason_id = rd.id " +
//                "join qrs q ON u.id = q.owner_id " +
//                "where q.id = ? and TIMESTAMPADD(SECOND, ?, n.created) >= NOW()";
//
//        return findOptionalNotification(sql, new Object[] {id, SECONDS_DELAY});
//    }

    private Optional<Notification> findOptionalNotification(String sql, SqlParameterSource paramSource) {
        return findOptionalNotification(sql, paramSource, new NotificationRowMapper());
    }

    private Optional<Notification> findOptionalNotification(String sql, SqlParameterSource paramSource, RowMapper<Notification> rowMapper) {
        try {
            return Optional.ofNullable(namedParameterJdbcTemplate.queryForObject(
                    sql,
                    paramSource,
                    rowMapper
            ));
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    private SqlParameterSource paramSource(Notification entity) {
        BeanPropertySqlParameterSource paramSource = new BeanPropertySqlParameterSource(entity);
        paramSource.registerSqlType("status", Types.VARCHAR);
        return paramSource;
    }

    public Integer findCountByUserIdAndStatus(Long userId, NotificationStatus notificationStatus) {
        MapSqlParameterSource param = new MapSqlParameterSource("userId", userId).addValue("status", notificationStatus.name());
        return namedParameterJdbcTemplate.queryForObject(SELECT_COUNT_BY_USER_ID_AND_STATUS, param, Integer.class);
    }

    public Integer findCountBySenderIdAndStatus(Long senderId, NotificationStatus notificationStatus) {
        MapSqlParameterSource param = new MapSqlParameterSource("senderId", senderId).addValue("status", notificationStatus.name());
        return namedParameterJdbcTemplate.queryForObject(SELECT_COUNT_BY_SENDER_ID_AND_STATUS, param, Integer.class);
    }

    public Integer findCountByUserId(Long userId) {
        MapSqlParameterSource param = new MapSqlParameterSource("userId", userId)
                .addValue("draft", NotificationStatus.DRAFT.name());
        return namedParameterJdbcTemplate.queryForObject(SELECT_COUNT_BY_USER_ID, param, Integer.class);
    }

    public Integer findCountBySenderId(Long senderId) {
        MapSqlParameterSource param = new MapSqlParameterSource("senderId", senderId)
                .addValue("draft", NotificationStatus.DRAFT.name());
        return namedParameterJdbcTemplate.queryForObject(SELECT_COUNT_BY_SENDER_ID, param, Integer.class);
    }

    public List<Notification> findPageByUserId(Long userId, PageParam page) {
        MapSqlParameterSource param = new MapSqlParameterSource("userId", userId)
                .addValue("offset", page.getSize() * page.getPage())
                .addValue("page_size", page.getSize())
                .addValue("draft", NotificationStatus.DRAFT.name());
        return namedParameterJdbcTemplate.query(SELECT_ALL_BY_PAGE_AND_USER_ID, param, new NotificationRowMapper());
    }

    public List<Notification> findPageBySenderId(Long senderId, PageParam page) {
        MapSqlParameterSource param = new MapSqlParameterSource("senderId", senderId)
                .addValue("offset", page.getSize() * page.getPage())
                .addValue("page_size", page.getSize())
                .addValue("draft", NotificationStatus.DRAFT.name());
        return namedParameterJdbcTemplate.query(SELECT_ALL_BY_PAGE_AND_SENDER_ID, param, new NotificationRowMapper());
    }

    public List<Notification> findPageByUserIdAndStatus(Long userId, NotificationStatus status, PageParam page) {
        MapSqlParameterSource param = new MapSqlParameterSource("userId", userId)
                .addValue("offset", page.getSize() * page.getPage())
                .addValue("page_size", page.getSize())
                .addValue("status", status.name());
        return namedParameterJdbcTemplate.query(SELECT_ALL_BY_STATUS_AND_USER_ID, param, new NotificationRowMapper());
    }

    public List<Notification> findPageBySenderIdAndStatus(Long senderId, NotificationStatus status, PageParam page) {
        MapSqlParameterSource param = new MapSqlParameterSource("senderId", senderId)
                .addValue("offset", page.getSize() * page.getPage())
                .addValue("page_size", page.getSize())
                .addValue("status", status.name());
        return namedParameterJdbcTemplate.query(SELECT_ALL_BY_STATUS_AND_SENDER_ID, param, new NotificationRowMapper());
    }

    public boolean deleteById(UUID id) {
        MapSqlParameterSource paramSource = new MapSqlParameterSource("id", id);
        return namedParameterJdbcTemplate.update(DELETE_BY_ID, paramSource) == 1;
    }

    public boolean deleteByQrId(UUID qrId) {
        MapSqlParameterSource paramSource = new MapSqlParameterSource("qrId", qrId);
        return namedParameterJdbcTemplate.update(DELETE_BY_QR_ID, paramSource) == 1;
    }

    public Optional<Notification> findByQrIdAndDateAfter(UUID qrId, LocalDateTime createdDate) {
        MapSqlParameterSource paramSource = new MapSqlParameterSource("qrId", qrId)
                .addValue("createdDate", createdDate);
        return findOptionalNotification(SELECT_BY_QR_ID_AND_CREATED_DATE, paramSource, BeanPropertyRowMapper.newInstance(Notification.class));
    }

    public Integer findCountByVisitorIdAndDateAfter(String visitorId, LocalDateTime createdDate) {
        MapSqlParameterSource paramSource = new MapSqlParameterSource("visitorId", visitorId)
                .addValue("createdDate", createdDate);
        return namedParameterJdbcTemplate.queryForObject(SELECT_COUNT_BY_VISITOR_ID_AND_CREATED_DATE, paramSource, Integer.class);
    }

    public void deleteAllByUserId(Long userId) {
        MapSqlParameterSource paramSource = new MapSqlParameterSource("userId", userId);
        namedParameterJdbcTemplate.update(DELETE_BY_USER_ID, paramSource);
    }
}
