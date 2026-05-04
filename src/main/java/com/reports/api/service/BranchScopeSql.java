package com.reports.api.service;

/**
 * Joins fact rows (alias {@code t}) to {@code branches} and the SBU hierarchy, then applies
 * optional hierarchy filters and {@code user_scope_assignments} rules — aligned with
 * {@link FilterService} branch scope logic.
 * <p>
 * All dimension tables are qualified with {@code main.} so JDBC resolves them even when the
 * connection {@code search_path} does not include {@code main}.
 * <p>
 * Requires fact rows to expose {@code branch_id} compatible with {@code main.branches.code}
 * (branch codes; use cast if types differ).
 */
public final class BranchScopeSql {

    private BranchScopeSql() {
    }

    public static final String JOIN_BRANCH_DIM_FROM_ALIAS_T = """
            inner join main.branches b on b.code = cast(t.branch_id as text)
            inner join main.units u on u.id = b.unit_id
            inner join main.regions rgn on rgn.id = u.region_id
            inner join main.clusters c on c.id = rgn.cluster_id
            inner join main.zones z on z.id = c.zone_id
            """;

    public static final String WHERE_FILTERS_AND_USER_SCOPE = """
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
                from main.user_scope_assignments usa
                where usa.user_id = cast(:userId as uuid)
                  and (
                    (usa.scope_type = 'branch' and usa.scope_id = b.id)
                    or (usa.scope_type = 'unit' and b.unit_id = usa.scope_id)
                    or (usa.scope_type = 'region' and b.unit_id in (select u2.id from main.units u2 where u2.region_id = usa.scope_id))
                    or (usa.scope_type = 'cluster' and b.unit_id in (
                          select u2.id from main.units u2 join main.regions r2 on r2.id = u2.region_id where r2.cluster_id = usa.scope_id))
                    or (usa.scope_type = 'zone' and b.unit_id in (
                          select u2.id
                          from main.units u2
                          join main.regions r2 on r2.id = u2.region_id
                          join main.clusters c2 on c2.id = r2.cluster_id
                          where c2.zone_id = usa.scope_id))
                    or (usa.scope_type = 'sbu' and b.unit_id in (
                          select u2.id
                          from main.units u2
                          join main.regions r2 on r2.id = u2.region_id
                          join main.clusters c2 on c2.id = r2.cluster_id
                          join main.zones z2 on z2.id = c2.zone_id
                          where z2.sbu_id = usa.scope_id))
                  )
              )
            """;

    /**
     * Wraps a validated single-statement SELECT so exports only include rows whose branch is in scope.
     * Inner SQL should qualify the fact table if needed, e.g. {@code select * from main.tdw_loan_outstanding_information}.
     */
    public static String wrapExportSelect(String innerExportSql) {
        return """
                select t.*
                from (
                %s
                ) t
                """.formatted(innerExportSql)
                + JOIN_BRANCH_DIM_FROM_ALIAS_T
                + WHERE_FILTERS_AND_USER_SCOPE;
    }
}
