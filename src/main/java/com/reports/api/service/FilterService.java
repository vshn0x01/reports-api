package com.reports.api.service;

import com.reports.api.dto.FilterOption;
import com.reports.api.dto.FilterOptionsResponse;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class FilterService {
    private final NamedParameterJdbcTemplate jdbcTemplate;

    public FilterService(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public FilterOptionsResponse getFilterOptions(
            UUID userId,
            Long sbuId,
            Long zoneId,
            Long clusterId,
            Long regionId,
            Long unitId,
            Long branchId,
            String branchCode
    ) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("userId", userId)
                .addValue("sbuId", sbuId)
                .addValue("zoneId", zoneId)
                .addValue("clusterId", clusterId)
                .addValue("regionId", regionId)
                .addValue("unitId", unitId)
                .addValue("branchId", branchId)
                .addValue("branchCode", branchCode);

        return new FilterOptionsResponse(
                querySbu(params),
                queryZones(params),
                queryClusters(params),
                queryRegions(params),
                queryUnits(params),
                queryBranches(params)
        );
    }

    /**
     * Access is driven by {@code user_scope_assignments (scope_type, scope_id)} only.
     * {@code scope_type} values: {@code sbu}, {@code zone}, {@code cluster}, {@code region}, {@code unit}, {@code branch}.
     */
    private List<FilterOption> querySbu(MapSqlParameterSource params) {
        String sql = """
                select distinct s.id, s.code, s.name
                from sbu s
                where exists (
                    select 1
                    from user_scope_assignments usa
                    where usa.user_id = cast(:userId as uuid)
                      and (
                        (usa.scope_type = 'sbu' and usa.scope_id = s.id)
                        or (usa.scope_type = 'zone' and exists (
                              select 1 from zones z where z.id = usa.scope_id and z.sbu_id = s.id))
                        or (usa.scope_type = 'cluster' and exists (
                              select 1
                              from clusters c
                              join zones z on z.id = c.zone_id
                              where c.id = usa.scope_id and z.sbu_id = s.id))
                        or (usa.scope_type = 'region' and exists (
                              select 1
                              from regions rgn
                              join clusters c on c.id = rgn.cluster_id
                              join zones z on z.id = c.zone_id
                              where rgn.id = usa.scope_id and z.sbu_id = s.id))
                        or (usa.scope_type = 'unit' and exists (
                              select 1
                              from units un
                              join regions rgn on rgn.id = un.region_id
                              join clusters c on c.id = rgn.cluster_id
                              join zones z on z.id = c.zone_id
                              where un.id = usa.scope_id and z.sbu_id = s.id))
                        or (usa.scope_type = 'branch' and exists (
                              select 1
                              from branches b
                              join units un on un.id = b.unit_id
                              join regions rgn on rgn.id = un.region_id
                              join clusters c on c.id = rgn.cluster_id
                              join zones z on z.id = c.zone_id
                              where b.id = usa.scope_id and z.sbu_id = s.id))
                      )
                )
                order by s.name
                """;
        return queryOptions(sql, params);
    }

    private List<FilterOption> queryZones(MapSqlParameterSource params) {
        String sql = """
                select distinct z.id, z.code, z.name
                from zones z
                where (cast(:sbuId as bigint) is null or z.sbu_id = cast(:sbuId as bigint))
                  and exists (
                    select 1
                    from user_scope_assignments usa
                    where usa.user_id = cast(:userId as uuid)
                      and (
                        (usa.scope_type = 'zone' and usa.scope_id = z.id)
                        or (usa.scope_type = 'sbu' and z.sbu_id = usa.scope_id)
                        or (usa.scope_type = 'cluster' and exists (
                              select 1 from clusters c where c.id = usa.scope_id and c.zone_id = z.id))
                        or (usa.scope_type = 'region' and exists (
                              select 1
                              from regions rgn
                              join clusters c on c.id = rgn.cluster_id
                              where rgn.id = usa.scope_id and c.zone_id = z.id))
                        or (usa.scope_type = 'unit' and exists (
                              select 1
                              from units un
                              join regions rgn on rgn.id = un.region_id
                              join clusters c on c.id = rgn.cluster_id
                              where un.id = usa.scope_id and c.zone_id = z.id))
                        or (usa.scope_type = 'branch' and exists (
                              select 1
                              from branches b
                              join units un on un.id = b.unit_id
                              join regions rgn on rgn.id = un.region_id
                              join clusters c on c.id = rgn.cluster_id
                              where b.id = usa.scope_id and c.zone_id = z.id))
                      )
                  )
                order by z.name
                """;
        return queryOptions(sql, params);
    }

    private List<FilterOption> queryClusters(MapSqlParameterSource params) {
        String sql = """
                select distinct c.id, c.code, c.name
                from clusters c
                join zones z on z.id = c.zone_id
                where (cast(:sbuId as bigint) is null or z.sbu_id = cast(:sbuId as bigint))
                  and (cast(:zoneId as bigint) is null or c.zone_id = cast(:zoneId as bigint))
                  and exists (
                    select 1
                    from user_scope_assignments usa
                    where usa.user_id = cast(:userId as uuid)
                      and (
                        (usa.scope_type = 'cluster' and usa.scope_id = c.id)
                        or (usa.scope_type = 'zone' and c.zone_id = usa.scope_id)
                        or (usa.scope_type = 'sbu' and c.zone_id in (select z2.id from zones z2 where z2.sbu_id = usa.scope_id))
                        or (usa.scope_type = 'region' and exists (
                              select 1 from regions rgn where rgn.id = usa.scope_id and rgn.cluster_id = c.id))
                        or (usa.scope_type = 'unit' and exists (
                              select 1
                              from units un
                              join regions rgn on rgn.id = un.region_id
                              where un.id = usa.scope_id and rgn.cluster_id = c.id))
                        or (usa.scope_type = 'branch' and exists (
                              select 1
                              from branches b
                              join units un on un.id = b.unit_id
                              join regions rgn on rgn.id = un.region_id
                              where b.id = usa.scope_id and rgn.cluster_id = c.id))
                      )
                  )
                order by c.name
                """;
        return queryOptions(sql, params);
    }

    private List<FilterOption> queryRegions(MapSqlParameterSource params) {
        String sql = """
                select distinct rgn.id, rgn.code, rgn.name
                from regions rgn
                join clusters c on c.id = rgn.cluster_id
                join zones z on z.id = c.zone_id
                where (cast(:sbuId as bigint) is null or z.sbu_id = cast(:sbuId as bigint))
                  and (cast(:zoneId as bigint) is null or c.zone_id = cast(:zoneId as bigint))
                  and (cast(:clusterId as bigint) is null or rgn.cluster_id = cast(:clusterId as bigint))
                  and exists (
                    select 1
                    from user_scope_assignments usa
                    where usa.user_id = cast(:userId as uuid)
                      and (
                        (usa.scope_type = 'region' and usa.scope_id = rgn.id)
                        or (usa.scope_type = 'cluster' and rgn.cluster_id = usa.scope_id)
                        or (usa.scope_type = 'zone' and rgn.cluster_id in (select c2.id from clusters c2 where c2.zone_id = usa.scope_id))
                        or (usa.scope_type = 'sbu' and rgn.cluster_id in (
                              select c2.id from clusters c2 join zones z2 on z2.id = c2.zone_id where z2.sbu_id = usa.scope_id))
                        or (usa.scope_type = 'unit' and exists (
                              select 1 from units un where un.id = usa.scope_id and un.region_id = rgn.id))
                        or (usa.scope_type = 'branch' and exists (
                              select 1
                              from branches b
                              join units un on un.id = b.unit_id
                              where b.id = usa.scope_id and un.region_id = rgn.id))
                      )
                  )
                order by rgn.name
                """;
        return queryOptions(sql, params);
    }

    private List<FilterOption> queryUnits(MapSqlParameterSource params) {
        String sql = """
                select distinct u.id, u.code, u.name
                from units u
                join regions rgn on rgn.id = u.region_id
                join clusters c on c.id = rgn.cluster_id
                join zones z on z.id = c.zone_id
                where (cast(:sbuId as bigint) is null or z.sbu_id = cast(:sbuId as bigint))
                  and (cast(:zoneId as bigint) is null or c.zone_id = cast(:zoneId as bigint))
                  and (cast(:clusterId as bigint) is null or rgn.cluster_id = cast(:clusterId as bigint))
                  and (cast(:regionId as bigint) is null or u.region_id = cast(:regionId as bigint))
                  and exists (
                    select 1
                    from user_scope_assignments usa
                    where usa.user_id = cast(:userId as uuid)
                      and (
                        (usa.scope_type = 'unit' and usa.scope_id = u.id)
                        or (usa.scope_type = 'region' and u.region_id = usa.scope_id)
                        or (usa.scope_type = 'cluster' and u.region_id in (
                              select r2.id from regions r2 where r2.cluster_id = usa.scope_id))
                        or (usa.scope_type = 'zone' and u.region_id in (
                              select r2.id
                              from regions r2
                              join clusters c2 on c2.id = r2.cluster_id
                              where c2.zone_id = usa.scope_id))
                        or (usa.scope_type = 'sbu' and u.region_id in (
                              select r2.id
                              from regions r2
                              join clusters c2 on c2.id = r2.cluster_id
                              join zones z2 on z2.id = c2.zone_id
                              where z2.sbu_id = usa.scope_id))
                        or (usa.scope_type = 'branch' and exists (
                              select 1 from branches b where b.id = usa.scope_id and b.unit_id = u.id))
                      )
                  )
                order by u.name
                """;
        return queryOptions(sql, params);
    }

    private List<FilterOption> queryBranches(MapSqlParameterSource params) {
        String sql = """
                select distinct b.id, b.code, b.name
                from branches b
                join units u on u.id = b.unit_id
                join regions rgn on rgn.id = u.region_id
                join clusters c on c.id = rgn.cluster_id
                join zones z on z.id = c.zone_id
                where (cast(:sbuId as bigint) is null or z.sbu_id = cast(:sbuId as bigint))
                  and (cast(:zoneId as bigint) is null or c.zone_id = cast(:zoneId as bigint))
                  and (cast(:clusterId as bigint) is null or rgn.cluster_id = cast(:clusterId as bigint))
                  and (cast(:regionId as bigint) is null or u.region_id = cast(:regionId as bigint))
                  and (cast(:unitId as bigint) is null or b.unit_id = cast(:unitId as bigint))
                  and (cast(:branchId as bigint) is null or b.id = cast(:branchId as bigint))
                  and (
                    coalesce(nullif(trim(cast(:branchCode as text)), ''), '') = ''
                    or trim(b.code) = trim(cast(:branchCode as text))
                  )
                  and exists (
                    select 1
                    from user_scope_assignments usa
                    where usa.user_id = cast(:userId as uuid)
                      and (
                        (usa.scope_type = 'branch' and usa.scope_id = b.id)
                        or (usa.scope_type = 'unit' and b.unit_id = usa.scope_id)
                        or (usa.scope_type = 'region' and b.unit_id in (select u2.id from units u2 where u2.region_id = usa.scope_id))
                        or (usa.scope_type = 'cluster' and b.unit_id in (
                              select u2.id from units u2 join regions r2 on r2.id = u2.region_id where r2.cluster_id = usa.scope_id))
                        or (usa.scope_type = 'zone' and b.unit_id in (
                              select u2.id
                              from units u2
                              join regions r2 on r2.id = u2.region_id
                              join clusters c2 on c2.id = r2.cluster_id
                              where c2.zone_id = usa.scope_id))
                        or (usa.scope_type = 'sbu' and b.unit_id in (
                              select u2.id
                              from units u2
                              join regions r2 on r2.id = u2.region_id
                              join clusters c2 on c2.id = r2.cluster_id
                              join zones z2 on z2.id = c2.zone_id
                              where z2.sbu_id = usa.scope_id))
                      )
                  )
                order by b.name
                """;
        return queryOptions(sql, params);
    }

    private List<FilterOption> queryOptions(String sql, MapSqlParameterSource params) {
        return jdbcTemplate.query(
                sql,
                params,
                (rs, rowNum) -> new FilterOption(
                        rs.getLong("id"),
                        rs.getString("code"),
                        rs.getString("name")
                )
        );
    }
}
