-- Open timetable_sprint1 in pgAdmin. Execute this entire file as one script.
-- Re-running keeps existing module records, exam settings, and matching slots.
-- Dummy dates: December 2026. Historical durations: University of Ruhuna Final TT 1357,
-- pages 1–7. Only unambiguous two- and three-hour records are seeded.
-- For ME3303 the module seed uses UNSPECIFIED because the original curriculum is blank.
-- Source dates and room assignments are not carried over.
-- Review before adding: CE1101 (60 min), MN3206 (240 min), ME5303 C-23
-- (curriculum printed as C123), and CE2201 C-18 (curriculum printed as C-183).
-- The script does not insert student registrations or group memberships.
BEGIN;

DO $$
BEGIN
  IF current_database() <> 'timetable_sprint1' THEN
    RAISE EXCEPTION 'Expected timetable_sprint1, connected to %', current_database();
  END IF;
END $$;

CREATE TEMP TABLE sprint1_exam_seed (
  module_code text NOT NULL,
  module_name text NOT NULL,
  curriculum text NOT NULL,
  semester_number integer NOT NULL,
  duration_minutes integer NOT NULL CHECK (duration_minutes IN (120, 180)),
  pdf_page integer NOT NULL,
  PRIMARY KEY (module_code, module_name, curriculum, semester_number)
) ON COMMIT DROP;

INSERT INTO sprint1_exam_seed VALUES
  ('CE1202', 'Introduction to Infrastructure Planning', 'C-23', 1, 180, 2),
  ('EE1101', 'Computer Programming I', 'C-18', 1, 120, 5),
  ('EE1102', 'Programming Fundamentals', 'C-23', 1, 120, 5),
  ('EE1301', 'Fundamentals of Electricity', 'C-23', 1, 180, 6),
  ('EE1302', 'Introduction to Electrical Engineering', 'C-18', 1, 180, 6),
  ('IS1003', 'Proficiency in English', 'C-18', 1, 120, 7),
  ('IS1003', 'Proficiency in English', 'C-23', 1, 120, 7),
  ('IS1301', 'Communication for Engineers', 'C-23', 1, 180, 2),
  ('IS1402', 'Mathematical Fundamentals for Engineers', 'C-18', 1, 180, 1),
  ('IS1402', 'Mathematical Fundamentals for Engineers', 'C-23', 1, 180, 1),
  ('ME1201', 'Engineering Drawing (Group A-I)', 'C-23', 1, 180, 5),
  ('ME1201', 'Engineering Drawing (Group A-II)', 'C-23', 1, 180, 5),
  ('ME1201', 'Engineering Drawing (Group B-I)', 'C-23', 1, 180, 5),
  ('ME1201', 'Engineering Drawing (Group B-II)', 'C-18', 1, 180, 5),
  ('ME1201', 'Engineering Drawing (Group B-II)', 'C-23', 1, 180, 5),
  ('ME1202', 'Fundamentals of Thermodynamics', 'C-23', 1, 180, 3),
  ('CE2201', 'Fundamentals of Fluid Mechanics', 'C-23', 2, 180, 7),
  ('CE2302', 'Mechanics of Materials', 'C-18', 2, 180, 7),
  ('CE2302', 'Mechanics of Materials', 'C-23', 2, 180, 7),
  ('EE2201', 'Computer Programming II', 'C-18', 2, 120, 3),
  ('EE2201', 'Fundamentals of Electronics', 'C-23', 2, 180, 1),
  ('EE2202', 'Introduction to Electronic Engineering', 'C-18', 2, 180, 1),
  ('EE2202', 'Object Oriented Programming', 'C-23', 2, 120, 3),
  ('IS2401', 'Linear Algebra and Differential Equations', 'C-18', 2, 180, 5),
  ('IS2401', 'Linear Algebra and Differential Equations', 'C-23', 2, 180, 5),
  ('ME2201', 'Engineering Mechanics', 'C-23', 2, 180, 7),
  ('ME2201', 'Fundamentals of Engineering Thermodynamics', 'C-18', 2, 180, 7),
  ('ME2302', 'Fundamentals of Materials and Manufacturing Engineering', 'C-23', 2, 180, 7),
  ('ME2302', 'Introduction to Materials Science and Manufacturing Engineering', 'C-18', 2, 180, 7),
  ('CE3201', 'Concrete Technology', 'C-23', 3, 180, 4),
  ('CE3202', 'Construction Process and Technology', 'C-23', 3, 180, 3),
  ('CE3204', 'Structural Analysis I', 'C-18', 3, 180, 5),
  ('CE3205', 'Structural Analysis I', 'C-18', 3, 180, 5),
  ('CE3205', 'Structural Analysis I', 'C-23', 3, 180, 5),
  ('CE3302', 'Engineering Surveying', 'C-18', 3, 180, 2),
  ('CE3303', 'Engineering Surveying', 'C-23', 3, 180, 2),
  ('CE3303', 'Fluid Mechanics', 'C-18', 3, 180, 6),
  ('CE3304', 'Fluid Mechanics', 'C-23', 3, 180, 6),
  ('EC3202', 'Data Structures and Algorithms', 'C-23', 3, 180, 6),
  ('EC3203', 'Electrical and Electronic Measurements', 'C-23', 3, 180, 6),
  ('EC3301', 'Analog Electronics', 'C-23', 3, 180, 3),
  ('EC3305', 'Signals and Systems', 'C-23', 3, 180, 2),
  ('EE3202', 'Data Structures and Algorithms', 'C-23', 3, 180, 6),
  ('EE3203', 'Electrical and Electronic Measurements', 'C-18', 3, 180, 6),
  ('EE3203', 'Electrical and Electronic Measurements', 'C-23', 3, 180, 6),
  ('EE3205', 'Power and Energy', 'C-23', 3, 180, 1),
  ('EE3252', 'Electrical and Electronic Measurements', 'C-18', 3, 180, 6),
  ('EE3301', 'Analog Electronics', 'C-23', 3, 180, 3),
  ('EE3302', 'Data Structures and Algorithms', 'C-18', 3, 180, 6),
  ('EE3304', 'Engineering Electromagnetism', 'C-23', 3, 180, 5),
  ('EE3305', 'Signals And Systems', 'C-18', 3, 180, 2),
  ('EE3306', 'Signals and Systems', 'C-23', 3, 180, 2),
  ('EE3351', 'Data Structures and Algorithms', 'C-18', 3, 180, 6),
  ('EE3355', 'Signals And Systems', 'C-18', 3, 180, 2),
  ('IS3301', 'Basic Economics', 'C-18', 3, 180, 3),
  ('IS3301', 'Complex Analysis and Mathematical Transforms', 'C-23', 3, 180, 1),
  ('IS3303', 'Complex Analysis and Mathematical Transforms', 'C-18', 3, 180, 1),
  ('IS3307', 'Society and the Engineers', 'C-18', 3, 180, 4),
  ('IS3321', 'Fundamentals of Management for Engineers', 'C-23', 3, 180, 3),
  ('IS3322', 'Society and the Engineers', 'C-23', 3, 180, 4),
  ('ME3203', 'Manufacturing Processes and Practices', 'C-18', 3, 180, 4),
  ('ME3204', 'Manufacturing Processes and Practice', 'C-23', 3, 180, 4),
  ('ME3206', 'Strength of Materials', 'C-23', 3, 180, 5),
  ('ME3210', 'Principles and Applications of Microcontrollers', 'C-23', 3, 180, 6),
  ('ME3301', 'Applied Thermodynamics', 'C-23', 3, 180, 2),
  ('ME3303', 'Fluid Mechanic', 'UNSPECIFIED', 3, 180, 6),
  ('ME3304', 'Metallurgy for Engineers', 'C-18', 3, 180, 3),
  ('ME3305', 'Metallurgy for Engineers', 'C-23', 3, 180, 3),
  ('MN3201', 'Fundamentals of Naval Architecture', 'C-23', 3, 180, 6),
  ('MN3204', 'Maritime English -I', 'C-23', 3, 180, 6),
  ('MN3302', 'Engineering Knowledge (General) I', 'C-23', 3, 180, 2),
  ('MN3303', 'Engineering Knowledge (Motor) I', 'C-23', 3, 180, 4),
  ('MN3305', 'Hydrostatics and stability of Marine Vehicles', 'C-23', 3, 180, 6),
  ('CE5201', 'Building Services Engineering', 'C-23', 5, 180, 5),
  ('CE5202', 'Design of Steel Structures', 'C-18', 5, 180, 6),
  ('CE5202', 'Design of Steel Structures', 'C-23', 5, 180, 6),
  ('CE5204', 'Integrated Solid Waste Management', 'C-18', 5, 180, 2),
  ('CE5204', 'Structural Analysis III', 'C-23', 5, 180, 2),
  ('CE5205', 'Structural Analysis III', 'C-18', 5, 180, 2),
  ('CE5251', 'Design of Timber and Masonary Structures', 'C-18', 5, 180, 2),
  ('CE5251', 'Design of Timber and Masonary Structures', 'C-23', 5, 180, 2),
  ('CE5252', 'Remote sensing and GIS', 'C-18', 5, 180, 1),
  ('CE5253', 'Advanced Surveying', 'C-23', 5, 180, 1),
  ('CE5301', 'Construction Process and Technology', 'C-18', 5, 180, 4),
  ('CE5303', 'Hydraulic Engineering', 'C-18', 5, 180, 3),
  ('CE5303', 'Hydraulic Engineering', 'C-23', 5, 180, 3),
  ('CE5305', 'Traffic and Transport Engineering', 'C-23', 5, 180, 6),
  ('CE5306', 'Traffic and Transport Engineering', 'C-18', 5, 180, 6),
  ('EE5201', 'Communication Systems', 'C-23', 5, 180, 2),
  ('EE5202', 'Electronic Circuit Design', 'C-23', 5, 180, 3),
  ('EE5203', 'Machine Learning', 'C-23', 5, 180, 3),
  ('EE5207', 'Advanced Digital Communications', 'C-23', 5, 180, 4),
  ('EE5210', 'Hardware Description Language', 'C-23', 5, 120, 6),
  ('EE5212', 'Operating Systems', 'C-23', 5, 120, 6),
  ('EE5213', 'Power System Analysis', 'C-23', 5, 180, 6),
  ('EE5303', 'Power Electronics', 'C-18', 5, 180, 1),
  ('EE5304', 'Power Electronics', 'C-23', 5, 180, 1),
  ('EE5304', 'Power Systems II', 'C-18', 5, 180, 5),
  ('EE5305', 'Power Systems II', 'C-23', 5, 180, 5),
  ('IS5101', 'Engineering Ethics', 'C-18', 5, 180, 4),
  ('IS5101', 'Engineering Ethics', 'C-23', 5, 180, 4),
  ('IS5123', 'Project Management', 'C-23', 5, 120, 1),
  ('IS5221', 'Industrial Management', 'C-23', 5, 120, 2),
  ('IS5222', 'Organizational Behaviour and Human Resource Management', 'C-23', 5, 180, 1),
  ('IS5302', 'Financial Management', 'C-18', 5, 180, 4),
  ('IS5302', 'Numerical Methods', 'C-23', 5, 180, 4),
  ('IS5306', 'Numerical Methods', 'C-18', 5, 180, 4),
  ('ME5204', 'Production Planning and Control', 'C-18', 5, 180, 6),
  ('ME5204', 'Production Planning and Control', 'C-23', 5, 180, 6),
  ('ME5210', 'Electric and Hybrid Vehicle Engineering (TE)', 'C-23', 5, 180, 3),
  ('ME5301', 'Advanced Control Systems', 'C-18', 5, 180, 1),
  ('ME5301', 'Advanced Control Systems', 'C-23', 5, 180, 1),
  ('ME5302', 'Computer Aided Design', 'C-18', 5, 180, 2),
  ('ME5302', 'Computer Aided Design', 'C-23', 5, 180, 2),
  ('ME5303', 'Mechanical Engineering Design', 'C-18', 5, 180, 4),
  ('ME5305', 'Refrigeration and Air – Conditioning', 'C-18', 5, 180, 5),
  ('ME5305', 'Refrigeration and Air – Conditioning', 'C-23', 5, 180, 5),
  ('CE7202', 'Computer Analysis of Structures', 'C-18', 7, 180, 2),
  ('CE7256', 'Sustainable Built Environment Principles', 'C-18', 7, 120, 3),
  ('CE7257', 'Railway Geotechnics', 'C-18', 7, 180, 4),
  ('CE7303', 'Construction Environmental Management', 'C-18', 7, 180, 4),
  ('CE7304', 'Geotechnical Engineering Design', 'C-18', 7, 180, 1),
  ('EC7201', 'Information Security', 'C-18', 7, 120, 2),
  ('EC7203', 'Advanced Artificial Intelligence', 'C-18', 7, 120, 2),
  ('EC7204', 'Cloud Computing', 'C-18', 7, 120, 6),
  ('EC7205', 'Computer Vision and Image Processing', 'C-18', 7, 120, 1),
  ('EC7206', 'Digital Signal Processing', 'C-18', 7, 180, 4),
  ('EC7208', 'Optimization Techniques for Engineers', 'C-18', 7, 180, 3),
  ('EC7210', 'Robotics and Automation', 'C-18', 7, 180, 6),
  ('EC7211', 'Telecommunication Networks', 'C-18', 7, 180, 6),
  ('EC7212', 'Advanced Data Communication', 'C-18', 7, 180, 4),
  ('EE7203', 'Advanced Data Communication', 'C-18', 7, 180, 4),
  ('EE7204', 'Computer Vision and Image Processing', 'C-18', 7, 120, 1),
  ('EE7205', 'Digital Signal Processing', 'C-18', 7, 180, 4),
  ('EE7207', 'Electrical Installations I', 'C-18', 7, 180, 5),
  ('EE7209', 'Machine Learning', 'C-18', 7, 180, 3),
  ('EE7213', 'Power Electronic Application', 'C-18', 7, 180, 2),
  ('EE7215', 'Power System Protection', 'C-18', 7, 180, 1),
  ('EE7217', 'Telecommunication Networks', 'C-18', 7, 180, 6),
  ('EE7219', 'Information Security', 'C-18', 7, 120, 2),
  ('EE7220', 'Optimization Techniques for Engineers', 'C-18', 7, 180, 3),
  ('EE7221', 'Robotics and Automation', 'C-18', 7, 180, 6),
  ('EE7222', 'Cloud Computing', 'C-18', 7, 120, 6),
  ('ME7211', 'Energy Technology', 'C-18', 7, 180, 1),
  ('ME7212', 'Naval Architecture and Hull Design', 'C-18', 7, 180, 2),
  ('ME7216', 'Mobile Robots Design', 'C-18', 7, 180, 2),
  ('ME7217', 'Aerospace Engineering', 'C-18', 7, 180, 3),
  ('ME7218', 'Computational Fluid Dynamics for Design and Analysis of Engineering Systems', 'C-18', 7, 180, 2),
  ('ME7302', 'Heat and Mass Transfer', 'C-18', 7, 180, 3),
  ('ME7303', 'Production and Operations Management', 'C-18', 7, 180, 6);

DO $$
DECLARE
  target_name constant text := 'Sprint 1 Dummy Exam Period - December 2026';
  start_date constant date := DATE '2026-12-01';
  finish_date constant date := DATE '2026-12-31';
  period_key bigint;
  module_key bigint;
  semester_key bigint;
  requirement_key bigint;
  exam_row record;
BEGIN
  IF NOT EXISTS (SELECT 1 FROM public.calendar_period WHERE period_name = target_name) THEN
    INSERT INTO public.calendar_period (period_type, period_name, starting_date, end_date)
    VALUES ('EXAM', target_name, start_date, finish_date);
  END IF;

  SELECT period_id INTO STRICT period_key
  FROM public.calendar_period
  WHERE period_name = target_name
    AND period_type = 'EXAM' AND starting_date = start_date AND end_date = finish_date;

  IF EXISTS (
    SELECT 1 FROM public.calendar_period p
    WHERE p.period_id <> period_key
      AND upper(trim(p.period_type)) = 'EXAM'
      AND p.starting_date <= finish_date AND p.end_date >= start_date
  ) THEN
    RAISE EXCEPTION 'Another exam period overlaps December 2026. No seed changes were committed.';
  END IF;

  IF EXISTS (
    SELECT 1 FROM public.timeslot t
    WHERE t.period_id = period_key
      AND (
        t.date NOT BETWEEN start_date AND finish_date
        OR extract(isodow FROM t.date) NOT BETWEEN 1 AND 5
        OR t.date IN (DATE '2026-12-23', DATE '2026-12-25')
        OR t.start_time IS NULL OR t.end_time IS NULL
        OR t.end_time - t.start_time <> interval '30 minutes'
        OR extract(minute FROM t.start_time) NOT IN (0, 30)
        OR extract(second FROM t.start_time) <> 0
        OR t.timeslot <> to_char(t.date + t.start_time, 'HH24:MI')
        OR NOT (
          (t.start_time >= time '09:00' AND t.end_time <= time '12:00')
          OR (t.start_time >= time '13:30' AND t.end_time <= time '18:00')
        )
      )
  ) THEN
    RAISE EXCEPTION 'Existing timeslots do not match the December exam grid. No seed changes were committed.';
  END IF;

  FOR exam_row IN SELECT * FROM sprint1_exam_seed ORDER BY semester_number, module_code, module_name, curriculum LOOP
    BEGIN
      SELECT m.module_id, m.semester_id INTO STRICT module_key, semester_key
      FROM public.module m
      JOIN public.semester s ON s.semester_id = m.semester_id
      WHERE m.module_code = exam_row.module_code
        AND m.module_name = exam_row.module_name
        AND m.curriculum = exam_row.curriculum
        AND s.semester_name = exam_row.semester_number::text
        AND s.academic_year = '2025/2026';
    EXCEPTION WHEN no_data_found OR too_many_rows THEN
      RAISE EXCEPTION 'Expected one module for % / % / % in semester %, academic year 2025/2026',
        exam_row.module_code, exam_row.module_name, exam_row.curriculum, exam_row.semester_number;
    END;

    INSERT INTO public.calendar_period_semester (period_id, semester_id)
    SELECT period_key, semester_key
    WHERE NOT EXISTS (
      SELECT 1 FROM public.calendar_period_semester p
      WHERE p.period_id = period_key AND p.semester_id = semester_key
    );

    INSERT INTO public.event_requirement (module_id, requirement_type, period_id)
    SELECT module_key, 'EXAM', period_key
    WHERE NOT EXISTS (
      SELECT 1 FROM public.event_requirement e
      WHERE e.module_id = module_key AND e.requirement_type = 'EXAM' AND e.period_id = period_key
    );

    SELECT e.requirement_id INTO STRICT requirement_key FROM public.event_requirement e
    WHERE e.module_id = module_key AND e.requirement_type = 'EXAM' AND e.period_id = period_key;

    INSERT INTO public.exam_requirement (requirement_id, has_exam, exam_duration_minutes)
    VALUES (requirement_key, true, exam_row.duration_minutes)
    ON CONFLICT (requirement_id) DO NOTHING;
  END LOOP;

  -- 30-minute grid. Keep 12:00–13:30 free so consecutive slots cannot cross lunch.
  INSERT INTO public.timeslot (period_id, date, timeslot, start_time, end_time)
  SELECT period_key, d.day::date, to_char(d.day + i.slot * interval '30 minutes' + interval '9 hours', 'HH24:MI'),
    time '09:00' + i.slot * interval '30 minutes',
    time '09:00' + (i.slot + 1) * interval '30 minutes'
  FROM generate_series(start_date::timestamp, finish_date::timestamp, interval '1 day') AS d(day)
  CROSS JOIN generate_series(0, 17) AS i(slot)
  WHERE extract(isodow FROM d.day) BETWEEN 1 AND 5
    AND d.day::date NOT IN (DATE '2026-12-23', DATE '2026-12-25')
    AND i.slot NOT BETWEEN 6 AND 8
  ON CONFLICT (period_id, date, timeslot) DO NOTHING;

  IF (SELECT count(*) FROM public.timeslot t WHERE t.period_id = period_key) <> 315 THEN
    RAISE EXCEPTION 'Expected 315 exam timeslots. No seed changes were committed.';
  END IF;
END $$;

COMMIT;

SELECT p.period_id, p.period_name,
  (SELECT count(*) FROM public.calendar_period_semester s WHERE s.period_id = p.period_id) AS semesters,
  (SELECT count(DISTINCT t.date) FROM public.timeslot t WHERE t.period_id = p.period_id) AS exam_days,
  (SELECT count(*) FROM public.timeslot t WHERE t.period_id = p.period_id) AS timeslots,
  (SELECT count(*) FROM public.event_requirement e
   JOIN public.exam_requirement x ON x.requirement_id = e.requirement_id
   WHERE e.period_id = p.period_id AND e.requirement_type = 'EXAM' AND x.has_exam) AS active_exams
FROM public.calendar_period p
WHERE p.period_name = 'Sprint 1 Dummy Exam Period - December 2026';
