-- Run this entire file in the pgAdmin Query Tool for timetable_sprint1.
-- Source: seed-modules-158.sql. Existing exact module matches are kept.
BEGIN;
SET LOCAL search_path TO public;

DO $$
BEGIN
  IF current_database() <> 'timetable_sprint1' THEN
    RAISE EXCEPTION 'Open Query Tool for timetable_sprint1 (current database: %)', current_database();
  END IF;
END $$;

CREATE TEMP TABLE sprint1_module_seed ON COMMIT DROP AS
SELECT * FROM (VALUES
  ('IS1402', 'Mathematical Fundamentals for Engineers', 'IS', 4, 1, 'C-23'),
  ('IS1402', 'Mathematical Fundamentals for Engineers', 'IS', 4, 1, 'C-18'),
  ('CE5252', 'Remote sensing and GIS', 'CE', 2, 5, 'C-18'),
  ('CE5253', 'Advanced Surveying', 'CE', 2, 5, 'C-23'),
  ('CE7304', 'Geotechnical Engineering Design', 'CE', 3, 7, 'C-18'),
  ('EE3205', 'Power and Energy', 'EIE', 2, 3, 'C-23'),
  ('ME7211', 'Energy Technology', 'MME', 2, 7, 'C-18'),
  ('EC7205', 'Computer Vision and Image Processing', 'EC', 2, 7, 'C-18'),
  ('EE7204', 'Computer Vision and Image Processing', 'EIE', 2, 7, 'C-18'),
  ('IS5123', 'Project Management', 'IS', 1, 5, 'C-23'),
  ('IS3301', 'Complex Analysis and Mathematical Transforms', 'IS', 3, 3, 'C-23'),
  ('IS3303', 'Complex Analysis and Mathematical Transforms', 'IS', 3, 3, 'C-18'),
  ('IS5222', 'Organizational Behaviour and Human Resource Management', 'IS', 2, 5, 'C-23'),
  ('EE7215', 'Power System Protection', 'EIE', 2, 7, 'C-18'),
  ('ME5301', 'Advanced Control Systems', 'MME', 3, 5, 'C-23'),
  ('ME5301', 'Advanced Control Systems', 'MME', 3, 5, 'C-18'),
  ('EE5303', 'Power Electronics', 'EIE', 3, 5, 'C-18'),
  ('EE5304', 'Power Electronics', 'EIE', 3, 5, 'C-23'),
  ('EE2201', 'Fundamentals of Electronics', 'EIE', 2, 2, 'C-23'),
  ('EE2202', 'Introduction to Electronic Engineering', 'EIE', 2, 2, 'C-18'),
  ('CE1202', 'Introduction to Infrastructure Planning', 'CE', 2, 1, 'C-23'),
  ('ME7216', 'Mobile Robots Design', 'MME', 2, 7, 'C-18'),
  ('ME7212', 'Naval Architecture and Hull Design', 'MME', 2, 7, 'C-18'),
  ('CE5204', 'Structural Analysis III', 'CE', 2, 5, 'C-23'),
  ('CE5205', 'Structural Analysis III', 'CE', 2, 5, 'C-18'),
  ('EC7201', 'Information Security', 'EC', 2, 7, 'C-18'),
  ('EE7219', 'Information Security', 'EIE', 2, 7, 'C-18'),
  ('IS5221', 'Industrial Management', 'IS', 2, 5, 'C-23'),
  ('CE7202', 'Computer Analysis of Structures', 'CE', 2, 7, 'C-18'),
  ('ME3301', 'Applied Thermodynamics', 'MME', 3, 3, 'C-23'),
  ('EC3305', 'Signals and Systems', 'EC', 3, 3, 'C-23'),
  ('EE3306', 'Signals and Systems', 'EIE', 3, 3, 'C-23'),
  ('EE3305', 'Signals And Systems', 'EIE', 3, 3, 'C-18'),
  ('EE3355', 'Signals And Systems', 'EIE', 3, 3, 'C-18'),
  ('MN3302', 'Engineering Knowledge (General) I', 'MENA', 3, 3, 'C-23'),
  ('EE5201', 'Communication Systems', 'EIE', 2, 5, 'C-23'),
  ('CE3303', 'Engineering Surveying', 'CE', 3, 3, 'C-23'),
  ('CE3302', 'Engineering Surveying', 'CE', 3, 3, 'C-18'),
  ('IS1301', 'Communication for Engineers', 'IS', 3, 1, 'C-23'),
  ('ME5302', 'Computer Aided Design', 'MME', 3, 5, 'C-23'),
  ('ME5302', 'Computer Aided Design', 'MME', 3, 5, 'C-18'),
  ('CE5251', 'Design of Timber and Masonary Structures', 'CE', 2, 5, 'C-23'),
  ('CE5251', 'Design of Timber and Masonary Structures', 'CE', 2, 5, 'C-18'),
  ('EC7203', 'Advanced Artificial Intelligence', 'EC', 2, 7, 'C-18'),
  ('EE7213', 'Power Electronic Application', 'EIE', 2, 7, 'C-18'),
  ('ME7218', 'Computational Fluid Dynamics for Design and Analysis of Engineering Systems', 'MME', 2, 7, 'C-18'),
  ('CE5204', 'Integrated Solid Waste Management', 'CE', 2, 5, 'C-18'),
  ('IS3321', 'Fundamentals of Management for Engineers', 'IS', 3, 3, 'C-23'),
  ('IS3301', 'Basic Economics', 'IS', 3, 3, 'C-18'),
  ('ME7217', 'Aerospace Engineering', 'MME', 2, 7, 'C-18'),
  ('EE2202', 'Object Oriented Programming', 'EIE', 2, 2, 'C-23'),
  ('EE2201', 'Computer Programming II', 'EIE', 2, 2, 'C-18'),
  ('EE7209', 'Machine Learning', 'EIE', 2, 7, 'C-18'),
  ('EE5203', 'Machine Learning', 'EIE', 2, 5, 'C-23'),
  ('ME5210', 'Electric and Hybrid Vehicle Engineering (TE)', 'MME', 2, 5, 'C-23'),
  ('ME1202', 'Fundamentals of Thermodynamics', 'MME', 2, 1, 'C-23'),
  ('CE5303', 'Hydraulic Engineering', 'CE', 3, 5, 'C-23'),
  ('CE5303', 'Hydraulic Engineering', 'CE', 3, 5, 'C-18'),
  ('EE5202', 'Electronic Circuit Design', 'EIE', 2, 5, 'C-23'),
  ('CE3202', 'Construction Process and Technology', 'CE', 2, 3, 'C-23'),
  ('EC7208', 'Optimization Techniques for Engineers', 'EC', 2, 7, 'C-18'),
  ('EE7220', 'Optimization Techniques for Engineers', 'EIE', 2, 7, 'C-18'),
  ('ME7302', 'Heat and Mass Transfer', 'MME', 3, 7, 'C-18'),
  ('MN3206', 'Marine Engineering Drawing', 'MENA', 2, 3, 'C-23'),
  ('CE7256', 'Sustainable Built Environment Principles', 'CE', 2, 7, 'C-18'),
  ('EC3301', 'Analog Electronics', 'EC', 3, 3, 'C-23'),
  ('EE3301', 'Analog Electronics', 'EIE', 3, 3, 'C-23'),
  ('ME3305', 'Metallurgy for Engineers', 'MME', 3, 3, 'C-23'),
  ('ME3304', 'Metallurgy for Engineers', 'MME', 3, 3, 'C-18'),
  ('ME5303', 'Mechanical Engineering Design', 'MME', 3, 5, 'C-18'),
  ('ME5303', 'Mechanical Engineering Design', 'MME', 3, 5, 'C-23'),
  ('CE5301', 'Construction Process and Technology', 'CE', 3, 5, 'C-18'),
  ('IS5101', 'Engineering Ethics', 'IS', 1, 5, 'C-23'),
  ('IS5101', 'Engineering Ethics', 'IS', 1, 5, 'C-18'),
  ('CE3201', 'Concrete Technology', 'CE', 2, 3, 'C-23'),
  ('CE7303', 'Construction Environmental Management', 'CE', 3, 7, 'C-18'),
  ('ME3204', 'Manufacturing Processes and Practice', 'MME', 2, 3, 'C-23'),
  ('ME3203', 'Manufacturing Processes and Practices', 'MME', 2, 3, 'C-18'),
  ('EE5207', 'Advanced Digital Communications', 'EIE', 2, 5, 'C-23'),
  ('EE7203', 'Advanced Data Communication', 'EIE', 2, 7, 'C-18'),
  ('EC7212', 'Advanced Data Communication', 'EC', 2, 7, 'C-18'),
  ('IS3322', 'Society and the Engineers', 'IS', 3, 3, 'C-23'),
  ('IS3307', 'Society and the Engineers', 'IS', 3, 3, 'C-18'),
  ('EE7205', 'Digital Signal Processing', 'EIE', 2, 7, 'C-18'),
  ('EC7206', 'Digital Signal Processing', 'EC', 2, 7, 'C-18'),
  ('MN3303', 'Engineering Knowledge (Motor) I', 'MENA', 3, 3, 'C-23'),
  ('CE7257', 'Railway Geotechnics', 'CE', 2, 7, 'C-18'),
  ('IS5302', 'Numerical Methods', 'IS', 3, 5, 'C-23'),
  ('IS5306', 'Numerical Methods', 'IS', 3, 5, 'C-18'),
  ('IS5302', 'Financial Management', 'IS', 3, 5, 'C-18'),
  ('CE1101', 'Basic Concepts in Environmental Engineering', 'CE', 1, 1, 'C-23'),
  ('CE1101', 'Basic Concepts in Environmental Engineering', 'CE', 1, 1, 'C-18'),
  ('EE5304', 'Power Systems II', 'EIE', 3, 5, 'C-18'),
  ('EE5305', 'Power Systems II', 'EIE', 3, 5, 'C-23'),
  ('ME3206', 'Strength of Materials', 'MME', 2, 3, 'C-23'),
  ('EE3304', 'Engineering Electromagnetism', 'EIE', 3, 3, 'C-23'),
  ('CE5201', 'Building Services Engineering', 'CE', 2, 5, 'C-23'),
  ('EE1102', 'Programming Fundamentals', 'EIE', 1, 1, 'C-23'),
  ('EE1101', 'Computer Programming I', 'EIE', 1, 1, 'C-18'),
  ('CE3205', 'Structural Analysis I', 'CE', 2, 3, 'C-23'),
  ('CE3205', 'Structural Analysis I', 'CE', 2, 3, 'C-18'),
  ('CE3204', 'Structural Analysis I', 'CE', 2, 3, 'C-18'),
  ('IS2401', 'Linear Algebra and Differential Equations', 'IS', 4, 2, 'C-23'),
  ('IS2401', 'Linear Algebra and Differential Equations', 'IS', 4, 2, 'C-18'),
  ('ME5305', 'Refrigeration and Air – Conditioning', 'MME', 3, 5, 'C-23'),
  ('ME5305', 'Refrigeration and Air – Conditioning', 'MME', 3, 5, 'C-18'),
  ('EE7207', 'Electrical Installations I', 'EIE', 2, 7, 'C-18'),
  ('ME1201', 'Engineering Drawing (Group A-I)', 'MME', 2, 1, 'C-23'),
  ('ME1201', 'Engineering Drawing (Group A-II)', 'MME', 2, 1, 'C-23'),
  ('ME1201', 'Engineering Drawing (Group B-I)', 'MME', 2, 1, 'C-23'),
  ('ME1201', 'Engineering Drawing (Group B-II)', 'MME', 2, 1, 'C-23'),
  ('ME1201', 'Engineering Drawing (Group B-II)', 'MME', 2, 1, 'C-18'),
  ('EE5213', 'Power System Analysis', 'EIE', 2, 5, 'C-23'),
  ('CE5202', 'Design of Steel Structures', 'CE', 2, 5, 'C-23'),
  ('CE5202', 'Design of Steel Structures', 'CE', 2, 5, 'C-18'),
  ('EE5212', 'Operating Systems', 'EIE', 2, 5, 'C-23'),
  ('EE7221', 'Robotics and Automation', 'EIE', 2, 7, 'C-18'),
  ('EC7210', 'Robotics and Automation', 'EC', 2, 7, 'C-18'),
  ('ME3210', 'Principles and Applications of Microcontrollers', 'MME', 2, 3, 'C-23'),
  ('MN3204', 'Maritime English -I', 'MENA', 2, 3, 'C-23'),
  ('EE3203', 'Electrical and Electronic Measurements', 'EIE', 2, 3, 'C-23'),
  ('EE3203', 'Electrical and Electronic Measurements', 'EIE', 2, 3, 'C-18'),
  ('EC3203', 'Electrical and Electronic Measurements', 'EC', 2, 3, 'C-23'),
  ('EE3252', 'Electrical and Electronic Measurements', 'EIE', 2, 3, 'C-18'),
  ('MN3305', 'Hydrostatics and stability of Marine Vehicles', 'MENA', 3, 3, 'C-23'),
  ('EC7204', 'Cloud Computing', 'EC', 2, 7, 'C-18'),
  ('EE7222', 'Cloud Computing', 'EIE', 2, 7, 'C-18'),
  ('ME7303', 'Production and Operations Management', 'MME', 3, 7, 'C-18'),
  ('CE3304', 'Fluid Mechanics', 'CE', 3, 3, 'C-23'),
  ('CE3303', 'Fluid Mechanics', 'CE', 3, 3, 'C-18'),
  ('EE1301', 'Fundamentals of Electricity', 'EIE', 3, 1, 'C-23'),
  ('EE1302', 'Introduction to Electrical Engineering', 'EIE', 3, 1, 'C-18'),
  ('ME5204', 'Production Planning and Control', 'MME', 2, 5, 'C-18'),
  ('ME5204', 'Production Planning and Control', 'MME', 2, 5, 'C-23'),
  ('EE5210', 'Hardware Description Language', 'EIE', 2, 5, 'C-23'),
  ('EE3202', 'Data Structures and Algorithms', 'EIE', 2, 3, 'C-23'),
  ('EE3302', 'Data Structures and Algorithms', 'EIE', 3, 3, 'C-18'),
  ('EC3202', 'Data Structures and Algorithms', 'EC', 2, 3, 'C-23'),
  ('EE3351', 'Data Structures and Algorithms', 'EIE', 3, 3, 'C-18'),
  ('CE5305', 'Traffic and Transport Engineering', 'CE', 3, 5, 'C-23'),
  ('CE5306', 'Traffic and Transport Engineering', 'CE', 3, 5, 'C-18'),
  ('MN3201', 'Fundamentals of Naval Architecture', 'MENA', 2, 3, 'C-23'),
  ('ME3303', 'Fluid Mechanic', 'MME', 3, 3, 'UNSPECIFIED'),
  ('EE7217', 'Telecommunication Networks', 'EIE', 2, 7, 'C-18'),
  ('EC7211', 'Telecommunication Networks', 'EC', 2, 7, 'C-18'),
  ('CE2302', 'Mechanics of Materials', 'CE', 3, 2, 'C-23'),
  ('CE2302', 'Mechanics of Materials', 'CE', 3, 2, 'C-18'),
  ('ME2201', 'Engineering Mechanics', 'MME', 2, 2, 'C-23'),
  ('ME2201', 'Fundamentals of Engineering Thermodynamics', 'MME', 2, 2, 'C-18'),
  ('CE2201', 'Fundamentals of Fluid Mechanics', 'CE', 2, 2, 'C-23'),
  ('CE2201', 'Fundamentals of Fluid Mechanics', 'CE', 2, 2, 'C-18'),
  ('ME2302', 'Fundamentals of Materials and Manufacturing Engineering', 'MME', 3, 2, 'C-23'),
  ('ME2302', 'Introduction to Materials Science and Manufacturing Engineering', 'MME', 3, 2, 'C-18'),
  ('IS1003', 'Proficiency in English', 'IS', 0, 1, 'C-23'),
  ('IS1003', 'Proficiency in English', 'IS', 0, 1, 'C-18')
) AS v(code, name, dept, credit, sem, curriculum);

DO $$
DECLARE
  bad text;
BEGIN
  SELECT string_agg(dept, ', ' ORDER BY dept) INTO bad
  FROM (SELECT DISTINCT dept FROM sprint1_module_seed) seed
  WHERE (SELECT count(*) FROM public.department d WHERE d.department_name = seed.dept) <> 1;

  IF bad IS NOT NULL THEN
    RAISE EXCEPTION 'Expected exactly one department row for each of: %', bad;
  END IF;

  SELECT string_agg(sem::text, ', ' ORDER BY sem) INTO bad
  FROM (SELECT DISTINCT sem FROM sprint1_module_seed) seed
  WHERE (SELECT count(*) FROM public.semester s
         WHERE s.semester_name = seed.sem::text) <> 1;

  IF bad IS NOT NULL THEN
    RAISE EXCEPTION 'Expected exactly one semester row for each semester_name: %', bad;
  END IF;
END $$;

INSERT INTO public.module (module_code, module_name, department_id, credit, semester_id, curriculum)
SELECT v.code, v.name, d.department_id, v.credit, s.semester_id, v.curriculum
FROM sprint1_module_seed v
JOIN public.department d ON d.department_name = v.dept
JOIN public.semester s ON s.semester_name = v.sem::text
WHERE NOT EXISTS (
  SELECT 1
  FROM public.module m
  WHERE m.module_code = v.code
    AND m.module_name = v.name
    AND m.curriculum IS NOT DISTINCT FROM v.curriculum
);


DO $$
BEGIN
  IF EXISTS (SELECT 1 FROM public.student_group) OR EXISTS (SELECT 1 FROM public.module_student_group) THEN
    RAISE EXCEPTION 'Student group tables are already populated. Check existing rows before applying this seed.';
  END IF;
END $$;

-- Student group seed data (155 groups) + module_student_group links.
-- Run AFTER the semester and module inserts (module data from seed-modules-158.sql).
-- One student_group row per source row (a module can have several groups);
-- each group is linked to its module through module_student_group.
-- Rows with no student count in the sheet (3 rows) are skipped.

DO $$
DECLARE
  r      record;
  gid    bigint;
  mid    bigint;
  semid  bigint;
BEGIN
  FOR r IN
    SELECT * FROM (VALUES
  (1, 'IS1402', 'Mathematical Fundamentals for Engineers', 'C-23', 1, 547),
  (2, 'IS1402', 'Mathematical Fundamentals for Engineers', 'C-18', 1, 1),
  (3, 'CE5252', 'Remote sensing and GIS', 'C-18', 5, 3),
  (4, 'CE5253', 'Advanced Surveying', 'C-23', 5, 122),
  (5, 'CE7304', 'Geotechnical Engineering Design', 'C-18', 7, 124),
  (6, 'EE3205', 'Power and Energy', 'C-23', 3, 74),
  (7, 'ME7211', 'Energy Technology', 'C-18', 7, 81),
  (8, 'EC7205', 'Computer Vision and Image Processing', 'C-18', 7, 174),
  (9, 'EE7204', 'Computer Vision and Image Processing', 'C-18', 7, 13),
  (10, 'IS5123', 'Project Management', 'C-23', 5, 86),
  (11, 'IS3301', 'Complex Analysis and Mathematical Transforms', 'C-23', 3, 538),
  (12, 'IS3303', 'Complex Analysis and Mathematical Transforms', 'C-18', 3, 8),
  (13, 'IS5222', 'Organizational Behaviour and Human Resource Management', 'C-23', 5, 4),
  (14, 'EE7215', 'Power System Protection', 'C-18', 7, 36),
  (15, 'ME5301', 'Advanced Control Systems', 'C-23', 5, 99),
  (16, 'ME5301', 'Advanced Control Systems', 'C-18', 5, 1),
  (17, 'EE5303', 'Power Electronics', 'C-18', 5, 1),
  (18, 'EE5304', 'Power Electronics', 'C-23', 5, 75),
  (19, 'EE2201', 'Fundamentals of Electronics', 'C-23', 2, 553),
  (20, 'EE2202', 'Introduction to Electronic Engineering', 'C-18', 2, 15),
  (21, 'CE1202', 'Introduction to Infrastructure Planning', 'C-23', 1, 549),
  (22, 'ME7216', 'Mobile Robots Design', 'C-18', 7, 10),
  (23, 'ME7212', 'Naval Architecture and Hull Design', 'C-18', 7, 31),
  (24, 'CE5204', 'Structural Analysis III', 'C-23', 5, 124),
  (25, 'CE5205', 'Structural Analysis III', 'C-18', 5, 7),
  (26, 'EC7201', 'Information Security', 'C-18', 7, 195),
  (27, 'EE7219', 'Information Security', 'C-18', 7, 11),
  (28, 'IS5221', 'Industrial Management', 'C-23', 5, 19),
  (29, 'CE7202', 'Computer Analysis of Structures', 'C-18', 7, 122),
  (30, 'ME3301', 'Applied Thermodynamics', 'C-23', 3, 101),
  (31, 'EC3305', 'Signals and Systems', 'C-23', 3, 191),
  (32, 'EE3306', 'Signals and Systems', 'C-23', 3, 74),
  (33, 'EE3305', 'Signals And Systems', 'C-18', 3, 9),
  (34, 'EE3355', 'Signals And Systems', 'C-18', 3, 9),
  (35, 'MN3302', 'Engineering Knowledge (General) I', 'C-23', 3, 49),
  (36, 'EE5201', 'Communication Systems', 'C-23', 5, 75),
  (37, 'CE3303', 'Engineering Surveying', 'C-23', 3, 132),
  (38, 'CE3302', 'Engineering Surveying', 'C-18', 3, 10),
  (39, 'IS1301', 'Communication for Engineers', 'C-23', 1, 547),
  (40, 'ME5302', 'Computer Aided Design', 'C-23', 5, 99),
  (41, 'ME5302', 'Computer Aided Design', 'C-18', 5, 2),
  (42, 'CE5251', 'Design of Timber and Masonary Structures', 'C-23', 5, 112),
  (43, 'CE5251', 'Design of Timber and Masonary Structures', 'C-18', 5, 14),
  (44, 'EC7203', 'Advanced Artificial Intelligence', 'C-18', 7, 190),
  (45, 'EE7213', 'Power Electronic Application', 'C-18', 7, 15),
  (46, 'ME7218', 'Computational Fluid Dynamics for Design and Analysis of Engineering Systems', 'C-18', 7, 18),
  (47, 'CE5204', 'Integrated Solid Waste Management', 'C-18', 5, 12),
  (48, 'IS3321', 'Fundamentals of Management for Engineers', 'C-23', 3, 510),
  (49, 'IS3301', 'Basic Economics', 'C-18', 3, 14),
  (50, 'ME7217', 'Aerospace Engineering', 'C-18', 7, 6),
  (51, 'EE2202', 'Object Oriented Programming', 'C-23', 2, 554),
  (52, 'EE2201', 'Computer Programming II', 'C-18', 2, 6),
  (53, 'EE7209', 'Machine Learning', 'C-18', 7, 28),
  (54, 'EE5203', 'Machine Learning', 'C-23', 5, 75),
  (55, 'ME5210', 'Electric and Hybrid Vehicle Engineering (TE)', 'C-23', 5, 75),
  (56, 'ME1202', 'Fundamentals of Thermodynamics', 'C-23', 1, 549),
  (57, 'CE5303', 'Hydraulic Engineering', 'C-23', 5, 124),
  (58, 'CE5303', 'Hydraulic Engineering', 'C-18', 5, 7),
  (59, 'EE5202', 'Electronic Circuit Design', 'C-23', 5, 75),
  (60, 'CE3202', 'Construction Process and Technology', 'C-23', 3, 125),
  (61, 'EC7208', 'Optimization Techniques for Engineers', 'C-18', 7, 119),
  (62, 'EE7220', 'Optimization Techniques for Engineers', 'C-18', 7, 14),
  (63, 'ME7302', 'Heat and Mass Transfer', 'C-18', 7, 99),
  (64, 'MN3206', 'Marine Engineering Drawing', 'C-23', 3, 45),
  (65, 'CE7256', 'Sustainable Built Environment Principles', 'C-18', 7, 23),
  (66, 'EC3301', 'Analog Electronics', 'C-23', 3, 190),
  (67, 'EE3301', 'Analog Electronics', 'C-23', 3, 74),
  (68, 'ME3305', 'Metallurgy for Engineers', 'C-23', 3, 100),
  (69, 'ME3305', 'Metallurgy for Engineers', 'C-23', 3, 47),
  (70, 'ME3304', 'Metallurgy for Engineers', 'C-18', 3, 2),
  (71, 'ME5303', 'Mechanical Engineering Design', 'C-18', 5, 1),
  (72, 'ME5303', 'Mechanical Engineering Design', 'C-23', 5, 99),
  (73, 'CE5301', 'Construction Process and Technology', 'C-18', 5, 1),
  (74, 'IS5101', 'Engineering Ethics', 'C-23', 5, 298),
  (75, 'IS5101', 'Engineering Ethics', 'C-18', 5, 3),
  (76, 'CE3201', 'Concrete Technology', 'C-23', 3, 126),
  (77, 'CE7303', 'Construction Environmental Management', 'C-18', 7, 122),
  (78, 'ME3204', 'Manufacturing Processes and Practice', 'C-23', 3, 101),
  (79, 'ME3204', 'Manufacturing Processes and Practice', 'C-23', 3, 46),
  (80, 'ME3203', 'Manufacturing Processes and Practices', 'C-18', 3, 1),
  (81, 'EE5207', 'Advanced Digital Communications', 'C-23', 5, 10),
  (82, 'EE7203', 'Advanced Data Communication', 'C-18', 7, 8),
  (83, 'EC7212', 'Advanced Data Communication', 'C-18', 7, 39),
  (84, 'IS3322', 'Society and the Engineers', 'C-23', 3, 494),
  (85, 'IS3307', 'Society and the Engineers', 'C-18', 3, 2),
  (86, 'EE7205', 'Digital Signal Processing', 'C-18', 7, 22),
  (87, 'EC7206', 'Digital Signal Processing', 'C-18', 7, 11),
  (88, 'MN3303', 'Engineering Knowledge (Motor) I', 'C-23', 3, 47),
  (89, 'CE7257', 'Railway Geotechnics', 'C-18', 7, 105),
  (90, 'IS5302', 'Numerical Methods', 'C-23', 5, 298),
  (91, 'IS5306', 'Numerical Methods', 'C-18', 5, 11),
  (92, 'IS5302', 'Financial Management', 'C-18', 5, 1),
  (93, 'CE1101', 'Basic Concepts in Environmental Engineering', 'C-23', 1, 553),
  (94, 'CE1101', 'Basic Concepts in Environmental Engineering', 'C-18', 1, 4),
  (95, 'EE5304', 'Power Systems II', 'C-18', 5, 1),
  (96, 'EE5305', 'Power Systems II', 'C-23', 5, 75),
  (97, 'ME3206', 'Strength of Materials', 'C-23', 3, 100),
  (98, 'ME3206', 'Strength of Materials', 'C-23', 3, 47),
  (99, 'EE3304', 'Engineering Electromagnetism', 'C-23', 3, 74),
  (100, 'CE5201', 'Building Services Engineering', 'C-23', 5, 124),
  (101, 'EE1102', 'Programming Fundamentals', 'C-23', 1, 561),
  (102, 'EE1101', 'Computer Programming I', 'C-18', 1, 1),
  (103, 'CE3205', 'Structural Analysis I', 'C-23', 3, 126),
  (104, 'CE3205', 'Structural Analysis I', 'C-18', 3, 11),
  (105, 'CE3204', 'Structural Analysis I', 'C-18', 3, 8),
  (106, 'IS2401', 'Linear Algebra and Differential Equations', 'C-23', 2, 566),
  (107, 'IS2401', 'Linear Algebra and Differential Equations', 'C-18', 2, 7),
  (108, 'ME5305', 'Refrigeration and Air – Conditioning', 'C-23', 5, 99),
  (109, 'ME5305', 'Refrigeration and Air – Conditioning', 'C-18', 5, 1),
  (110, 'EE7207', 'Electrical Installations I', 'C-18', 7, 41),
  (111, 'ME1201', 'Engineering Drawing (Group A-I)', 'C-23', 1, 546),
  (112, 'ME1201', 'Engineering Drawing (Group B-II)', 'C-18', 1, 2),
  (113, 'EE5213', 'Power System Analysis', 'C-23', 5, 54),
  (114, 'CE5202', 'Design of Steel Structures', 'C-23', 5, 124),
  (115, 'CE5202', 'Design of Steel Structures', 'C-18', 5, 33),
  (116, 'EE5212', 'Operating Systems', 'C-23', 5, 13),
  (117, 'EE7221', 'Robotics and Automation', 'C-18', 7, 24),
  (118, 'EC7210', 'Robotics and Automation', 'C-18', 7, 73),
  (119, 'ME3210', 'Principles and Applications of Microcontrollers', 'C-23', 3, 6),
  (120, 'MN3204', 'Maritime English -I', 'C-23', 3, 45),
  (121, 'EE3203', 'Electrical and Electronic Measurements', 'C-23', 3, 74),
  (122, 'EE3203', 'Electrical and Electronic Measurements', 'C-18', 3, 5),
  (123, 'EC3203', 'Electrical and Electronic Measurements', 'C-23', 3, 190),
  (124, 'EE3252', 'Electrical and Electronic Measurements', 'C-18', 3, 3),
  (125, 'MN3305', 'Hydrostatics and stability of Marine Vehicles', 'C-23', 3, 45),
  (126, 'EC7204', 'Cloud Computing', 'C-18', 7, 174),
  (127, 'EE7222', 'Cloud Computing', 'C-18', 7, 2),
  (128, 'ME7303', 'Production and Operations Management', 'C-18', 7, 99),
  (129, 'CE3304', 'Fluid Mechanics', 'C-23', 3, 126),
  (130, 'CE3303', 'Fluid Mechanics', 'C-18', 3, 22),
  (131, 'EE1301', 'Fundamentals of Electricity', 'C-23', 1, 550),
  (132, 'EE1302', 'Introduction to Electrical Engineering', 'C-18', 1, 4),
  (133, 'ME5204', 'Production Planning and Control', 'C-18', 5, 7),
  (134, 'ME5204', 'Production Planning and Control', 'C-23', 5, 99),
  (135, 'EE5210', 'Hardware Description Language', 'C-23', 5, 37),
  (136, 'EE3202', 'Data Structures and Algorithms', 'C-23', 3, 74),
  (137, 'EE3302', 'Data Structures and Algorithms', 'C-18', 3, 5),
  (138, 'EC3202', 'Data Structures and Algorithms', 'C-23', 3, 192),
  (139, 'EE3351', 'Data Structures and Algorithms', 'C-18', 3, 1),
  (140, 'CE5305', 'Traffic and Transport Engineering', 'C-23', 5, 124),
  (141, 'CE5306', 'Traffic and Transport Engineering', 'C-18', 5, 20),
  (142, 'MN3201', 'Fundamentals of Naval Architecture', 'C-23', 3, 45),
  (143, 'ME3303', 'Fluid Mechanic', 'UNSPECIFIED', 3, 100),
  (144, 'EE7217', 'Telecommunication Networks', 'C-18', 7, 25),
  (145, 'EC7211', 'Telecommunication Networks', 'C-18', 7, 9),
  (146, 'CE2302', 'Mechanics of Materials', 'C-23', 2, 564),
  (147, 'CE2302', 'Mechanics of Materials', 'C-18', 2, 12),
  (148, 'ME2201', 'Engineering Mechanics', 'C-23', 2, 551),
  (149, 'ME2201', 'Fundamentals of Engineering Thermodynamics', 'C-18', 2, 2),
  (150, 'CE2201', 'Fundamentals of Fluid Mechanics', 'C-23', 2, 599),
  (151, 'CE2201', 'Fundamentals of Fluid Mechanics', 'C-18', 2, 16),
  (152, 'ME2302', 'Fundamentals of Materials and Manufacturing Engineering', 'C-23', 2, 575),
  (153, 'ME2302', 'Introduction to Materials Science and Manufacturing Engineering', 'C-18', 2, 3),
  (154, 'IS1003', 'Proficiency in English', 'C-23', 1, 588),
  (155, 'IS1003', 'Proficiency in English', 'C-18', 1, 11)
    ) AS v(seq, code, name, curriculum, sem, size)
    ORDER BY seq
  LOOP
    SELECT semester_id INTO semid FROM semester WHERE semester_name = r.sem::text;
    IF semid IS NULL THEN
      RAISE EXCEPTION 'Semester % not found', r.sem;
    END IF;

    SELECT module_id INTO mid FROM module
     WHERE module_code = r.code AND module_name = r.name
       AND curriculum IS NOT DISTINCT FROM r.curriculum;
    IF mid IS NULL THEN
      RAISE EXCEPTION 'Module not found: % % %', r.code, r.name, r.curriculum;
    END IF;

    INSERT INTO student_group (semester_id, group_size) VALUES (semid, r.size)
    RETURNING group_id INTO gid;

    INSERT INTO module_student_group (group_id, module_id) VALUES (gid, mid);
  END LOOP;
END $$;

COMMIT;

SELECT
  (SELECT count(*) FROM public.module WHERE module_code = 'IS1402' AND curriculum = 'C-23') AS is1402_c23,
  (SELECT count(*) FROM public.student_group) AS student_groups,
  (SELECT count(*) FROM public.module_student_group) AS module_group_links;
