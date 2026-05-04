-- Reports: per-report SQL for GET /reports/{id}/export/excel (single SELECT / WITH … SELECT)
alter table main.reports add column if not exists export_sql text;

-- Users: display fields for /auth/me (nullable until backfilled)
alter table main.users add column if not exists full_name varchar(255);
alter table main.users add column if not exists email varchar(255);

-- Required schema updates for hierarchy-aware scoped filtering.
-- This assumes your base tables already exist.

-- 1) Region scope support in user assignments
alter table user_scope_assignments
    add column if not exists region_id bigint references regions(id);

-- 2) Scope: prefer (scope_type, scope_id). Legacy sbu_id/zone_id/... may still exist; app reads scope_type + scope_id.
alter table user_scope_assignments
    drop constraint if exists chk_user_scope_any_not_null;

alter table user_scope_assignments
    add constraint chk_user_scope_any_not_null
    check (
        (scope_type is not null and scope_id is not null)
        or sbu_id is not null
        or zone_id is not null
        or cluster_id is not null
        or region_id is not null
        or unit_id is not null
        or branch_id is not null
    );

-- 3) Helpful indexes for filter API and scoped report queries
create index if not exists idx_user_scope_user_id on user_scope_assignments(user_id);
create index if not exists idx_user_scope_sbu_id on user_scope_assignments(sbu_id);
create index if not exists idx_user_scope_zone_id on user_scope_assignments(zone_id);
create index if not exists idx_user_scope_cluster_id on user_scope_assignments(cluster_id);
create index if not exists idx_user_scope_region_id on user_scope_assignments(region_id);
create index if not exists idx_user_scope_unit_id on user_scope_assignments(unit_id);
create index if not exists idx_user_scope_branch_id on user_scope_assignments(branch_id);

create index if not exists idx_zones_sbu_id on zones(sbu_id);
create index if not exists idx_clusters_zone_id on clusters(zone_id);
create index if not exists idx_regions_cluster_id on regions(cluster_id);
create index if not exists idx_units_region_id on units(region_id);
create index if not exists idx_branches_unit_id on branches(unit_id);

create index if not exists idx_report_role_access_role_id on report_role_access(role_id);
create index if not exists idx_report_runs_user_requested_at on report_runs(user_id, requested_at desc);
