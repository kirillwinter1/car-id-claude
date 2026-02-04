package ru.car.repository;


import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import ru.car.model.VersionControl;

@Repository
@RequiredArgsConstructor
public class VersionControlRepository {
    private static final String SELECT_BY_ID = "SELECT vc.* FROM version_control vc WHERE vc.id = 1";

    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    public VersionControl findFirst() {
        return namedParameterJdbcTemplate.queryForObject(
                SELECT_BY_ID,
                new MapSqlParameterSource(),
                BeanPropertyRowMapper.newInstance(VersionControl.class)
        );
    }


}
