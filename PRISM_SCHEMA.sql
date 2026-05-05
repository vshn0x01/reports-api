-- main.reports definition

-- Drop table

-- DROP TABLE main.reports;

CREATE TABLE main.reports (
	id uuid NOT NULL,
	code varchar(80) NOT NULL,
	"name" varchar(120) NOT NULL,
	description text NULL,
	category varchar(40) NOT NULL,
	cadence varchar(20) NOT NULL,
	output_format varchar(10) NOT NULL,
	is_active bool DEFAULT true NOT NULL,
	created_at timestamptz DEFAULT now() NOT NULL,
	export_sql text NULL,
	CONSTRAINT reports_code_key UNIQUE (code),
	CONSTRAINT reports_pkey PRIMARY KEY (id)
);


-- main.roles definition

-- Drop table

-- DROP TABLE main.roles;

CREATE TABLE main.roles (
	id smallserial NOT NULL,
	code varchar(40) NOT NULL,
	"name" varchar(80) NOT NULL,
	CONSTRAINT roles_code_key UNIQUE (code),
	CONSTRAINT roles_pkey PRIMARY KEY (id)
);


-- main.sbu definition

-- Drop table

-- DROP TABLE main.sbu;

CREATE TABLE main.sbu (
	id bigserial NOT NULL,
	code varchar(30) NOT NULL,
	"name" varchar(100) NOT NULL,
	CONSTRAINT sbu_code_key UNIQUE (code),
	CONSTRAINT sbu_pkey PRIMARY KEY (id)
);


-- main.tdw_loan_outstanding_information definition

-- Drop table

-- DROP TABLE main.tdw_loan_outstanding_information;

CREATE TABLE main.tdw_loan_outstanding_information (
	ason_date date NULL,
	system_date date NULL,
	sbu_id varchar(100) NULL,
	sbu_name varchar(100) NULL,
	zone_id text NULL,
	zone_name text NULL,
	cluster_id text NULL,
	cluster_name text NULL,
	region_id text NULL,
	region_name text NULL,
	unit_id text NULL,
	unit_name text NULL,
	branch_id text NULL,
	branch_name text NULL,
	customer_id text NULL,
	customer_name text NULL,
	mobile_no text NULL,
	loan_id text NULL,
	loan_status text NULL,
	product text NULL,
	legacy_loan_id text NULL,
	disbursement_date timestamp NULL,
	disbursement_amount numeric(15, 2) NULL,
	emi_amount numeric NULL,
	pos numeric NULL,
	ios numeric NULL,
	pos_od numeric NULL,
	ios_od numeric NULL,
	od_bucket text NULL
);


-- main.tdw_loan_outstnading_information definition

-- Drop table

-- DROP TABLE main.tdw_loan_outstnading_information;

CREATE TABLE main.tdw_loan_outstnading_information (
	ason_date date NULL,
	system_date date NULL,
	sbu_id varchar(100) NULL,
	sbu_name varchar(100) NULL,
	zone_id text NULL,
	zone_name text NULL,
	cluster_id text NULL,
	cluster_name text NULL,
	region_id text NULL,
	region_name text NULL,
	unit_id text NULL,
	unit_name text NULL,
	branch_id text NULL,
	branch_name text NULL,
	customer_id text NULL,
	customer_name text NULL,
	mobile_no text NULL,
	loan_id text NULL,
	loan_status text NULL,
	product text NULL,
	legacy_loan_id text NULL,
	disbursement_date timestamp NULL,
	disbursement_amount numeric(15, 2) NULL,
	emi_amount numeric NULL,
	pos numeric NULL,
	ios numeric NULL,
	pos_od numeric NULL,
	ios_od numeric NULL,
	od_bucket text NULL
);


-- main.users definition

-- Drop table

-- DROP TABLE main.users;

CREATE TABLE main.users (
	id uuid NOT NULL,
	username varchar(80) NOT NULL,
	email varchar(150) NOT NULL,
	full_name varchar(120) NOT NULL,
	password_hash text NOT NULL,
	is_active bool DEFAULT true NOT NULL,
	created_at timestamptz DEFAULT now() NOT NULL,
	updated_at timestamptz DEFAULT now() NOT NULL,
	CONSTRAINT users_email_key UNIQUE (email),
	CONSTRAINT users_pkey PRIMARY KEY (id),
	CONSTRAINT users_username_key UNIQUE (username)
);


-- main.report_role_access definition

-- Drop table

-- DROP TABLE main.report_role_access;

CREATE TABLE main.report_role_access (
	report_id uuid NOT NULL,
	role_id int2 NOT NULL,
	CONSTRAINT report_role_access_pkey PRIMARY KEY (report_id, role_id),
	CONSTRAINT report_role_access_report_id_fkey FOREIGN KEY (report_id) REFERENCES main.reports(id),
	CONSTRAINT report_role_access_role_id_fkey FOREIGN KEY (role_id) REFERENCES main.roles(id)
);


-- main.report_runs definition

-- Drop table

-- DROP TABLE main.report_runs;

CREATE TABLE main.report_runs (
	id uuid NOT NULL,
	user_id uuid NOT NULL,
	report_id uuid NOT NULL,
	status varchar(20) NOT NULL,
	requested_at timestamptz DEFAULT now() NOT NULL,
	completed_at timestamptz NULL,
	file_url text NULL,
	file_size_bytes int8 NULL,
	error_message text NULL,
	CONSTRAINT report_runs_pkey PRIMARY KEY (id),
	CONSTRAINT report_runs_report_id_fkey FOREIGN KEY (report_id) REFERENCES main.reports(id),
	CONSTRAINT report_runs_user_id_fkey FOREIGN KEY (user_id) REFERENCES main.users(id)
);


-- main.user_roles definition

-- Drop table

-- DROP TABLE main.user_roles;

CREATE TABLE main.user_roles (
	user_id uuid NOT NULL,
	role_id int2 NOT NULL,
	CONSTRAINT user_roles_pkey PRIMARY KEY (user_id, role_id),
	CONSTRAINT user_roles_role_id_fkey FOREIGN KEY (role_id) REFERENCES main.roles(id),
	CONSTRAINT user_roles_user_id_fkey FOREIGN KEY (user_id) REFERENCES main.users(id)
);


-- main.user_scope_assignments definition

-- Drop table

-- DROP TABLE main.user_scope_assignments;

CREATE TABLE main.user_scope_assignments (
	id bigserial NOT NULL,
	user_id uuid NOT NULL,
	created_at timestamptz DEFAULT now() NOT NULL,
	scope_type varchar(20) NULL,
	scope_id int8 NULL,
	CONSTRAINT user_scope_assignments_pkey PRIMARY KEY (id),
	CONSTRAINT user_scope_assignments_user_id_fkey FOREIGN KEY (user_id) REFERENCES main.users(id)
);


-- main.zones definition

-- Drop table

-- DROP TABLE main.zones;

CREATE TABLE main.zones (
	id bigserial NOT NULL,
	sbu_id int8 NOT NULL,
	code varchar(30) NOT NULL,
	"name" varchar(100) NOT NULL,
	CONSTRAINT zones_code_key UNIQUE (code),
	CONSTRAINT zones_pkey PRIMARY KEY (id),
	CONSTRAINT zones_sbu_id_fkey FOREIGN KEY (sbu_id) REFERENCES main.sbu(id)
);


-- main.clusters definition

-- Drop table

-- DROP TABLE main.clusters;

CREATE TABLE main.clusters (
	id bigserial NOT NULL,
	zone_id int8 NOT NULL,
	code varchar(30) NOT NULL,
	"name" varchar(100) NOT NULL,
	CONSTRAINT clusters_code_key UNIQUE (code),
	CONSTRAINT clusters_pkey PRIMARY KEY (id),
	CONSTRAINT clusters_zone_id_fkey FOREIGN KEY (zone_id) REFERENCES main.zones(id)
);


-- main.regions definition

-- Drop table

-- DROP TABLE main.regions;

CREATE TABLE main.regions (
	id bigserial NOT NULL,
	cluster_id int8 NOT NULL,
	code varchar(30) NOT NULL,
	"name" varchar(100) NOT NULL,
	CONSTRAINT regions_code_key UNIQUE (code),
	CONSTRAINT regions_pkey PRIMARY KEY (id),
	CONSTRAINT regions_cluster_id_fkey FOREIGN KEY (cluster_id) REFERENCES main.clusters(id)
);


-- main.report_run_filters definition

-- Drop table

-- DROP TABLE main.report_run_filters;

CREATE TABLE main.report_run_filters (
	report_run_id uuid NOT NULL,
	filter_key varchar(30) NOT NULL,
	filter_value text NOT NULL,
	CONSTRAINT report_run_filters_pkey PRIMARY KEY (report_run_id, filter_key, filter_value),
	CONSTRAINT report_run_filters_report_run_id_fkey FOREIGN KEY (report_run_id) REFERENCES main.report_runs(id) ON DELETE CASCADE
);


-- main.units definition

-- Drop table

-- DROP TABLE main.units;

CREATE TABLE main.units (
	id bigserial NOT NULL,
	region_id int8 NOT NULL,
	code varchar(30) NOT NULL,
	"name" varchar(100) NOT NULL,
	CONSTRAINT units_code_key UNIQUE (code),
	CONSTRAINT units_pkey PRIMARY KEY (id),
	CONSTRAINT units_region_id_fkey FOREIGN KEY (region_id) REFERENCES main.regions(id)
);


-- main.branches definition

-- Drop table

-- DROP TABLE main.branches;

CREATE TABLE main.branches (
	id bigserial NOT NULL,
	unit_id int8 NOT NULL,
	code varchar(30) NOT NULL,
	"name" varchar(120) NOT NULL,
	CONSTRAINT branches_code_key UNIQUE (code),
	CONSTRAINT branches_pkey PRIMARY KEY (id),
	CONSTRAINT branches_unit_id_fkey FOREIGN KEY (unit_id) REFERENCES main.units(id)
);