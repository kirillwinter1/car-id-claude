package ru.car.rowMapper;

import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.RowMapper;
import ru.car.model.Notification;
import ru.car.model.Qr;
import ru.car.model.ReasonDictionary;

import java.sql.ResultSet;
import java.sql.SQLException;

public class NotificationRowMapper implements RowMapper<Notification> {

    @Override
    public Notification mapRow(ResultSet rs, int rowNum) throws SQLException {
        Notification notification = (new BeanPropertyRowMapper<>(Notification.class)).mapRow(rs,rowNum);
        Qr qr = (new NestedRowMapper<>(Qr.class, "qr")).mapRow(rs,rowNum);
        notification.setQr(qr);
        ReasonDictionary rd = (new NestedRowMapper<>(ReasonDictionary.class, "rd")).mapRow(rs,rowNum);
        notification.setReason(rd);
        return notification;
    }
}
