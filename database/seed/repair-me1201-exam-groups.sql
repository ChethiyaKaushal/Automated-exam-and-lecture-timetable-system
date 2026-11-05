-- Open timetable_sprint1 in pgAdmin. Run the complete script.
-- Source: Final TT 1357, page 5 (Engineering Drawing C-23), and page 7 venue notes.
-- The existing 546 row is the total for all four subgroups, not the size of A-I.
BEGIN;

DO $$
DECLARE
  row_data record;
  module_key bigint;
  semester_key bigint;
  group_key bigint;
  link_count bigint;
  current_size integer;
BEGIN
  IF current_database() <> 'timetable_sprint1' THEN
    RAISE EXCEPTION 'Expected timetable_sprint1, connected to %', current_database();
  END IF;

  FOR row_data IN
    SELECT * FROM (VALUES
      ('Engineering Drawing (Group A-I)', 140, true),
      ('Engineering Drawing (Group A-II)', 140, false),
      ('Engineering Drawing (Group B-I)', 140, false),
      ('Engineering Drawing (Group B-II)', 126, false)
    ) AS v(module_name, group_size, was_aggregate)
  LOOP
    BEGIN
      SELECT m.module_id, m.semester_id INTO STRICT module_key, semester_key
      FROM public.module m
      JOIN public.semester s ON s.semester_id = m.semester_id
      WHERE m.module_code = 'ME1201'
        AND m.module_name = row_data.module_name
        AND m.curriculum = 'C-23'
        AND s.semester_name = '1'
        AND s.academic_year = '2025/2026';
    EXCEPTION WHEN no_data_found OR too_many_rows THEN
      RAISE EXCEPTION 'Expected exactly one ME1201 C-23 module for %', row_data.module_name;
    END;

    SELECT count(*), min(link.group_id) INTO link_count, group_key
    FROM public.module_student_group link
    WHERE link.module_id = module_key;

    IF link_count > 1 THEN
      RAISE EXCEPTION 'Module % already has multiple groups. Review its existing links.', module_key;
    ELSIF link_count = 0 THEN
      IF row_data.was_aggregate THEN
        RAISE EXCEPTION 'The original ME1201 A-I group was not found. Review the existing data.';
      END IF;

      INSERT INTO public.student_group (semester_id, group_size)
      VALUES (semester_key, row_data.group_size)
      RETURNING group_id INTO group_key;

      INSERT INTO public.module_student_group (module_id, group_id)
      VALUES (module_key, group_key);
    ELSE
      SELECT g.group_size INTO STRICT current_size
      FROM public.student_group g
      WHERE g.group_id = group_key AND g.semester_id = semester_key;

      IF current_size IS DISTINCT FROM row_data.group_size THEN
        IF NOT row_data.was_aggregate OR current_size IS DISTINCT FROM 546 THEN
          RAISE EXCEPTION 'Group % has unexpected size %. Review before changing it.', group_key, current_size;
        END IF;

        IF EXISTS (SELECT 1 FROM public.group_member gm WHERE gm.group_id = group_key)
          OR EXISTS (
            SELECT 1 FROM public.module_student_group link
            WHERE link.group_id = group_key AND link.module_id <> module_key
          )
          OR EXISTS (
            SELECT 1 FROM public.student_group_allocation allocation WHERE allocation.group_id = group_key
          )
        THEN
          RAISE EXCEPTION 'Group % has memberships, other module links, or allocations. Review before resizing it.', group_key;
        END IF;

        UPDATE public.student_group SET group_size = row_data.group_size
        WHERE group_id = group_key;
      END IF;
    END IF;
  END LOOP;
END $$;

COMMIT;

SELECT m.module_id, m.module_name, g.group_id, g.group_size
FROM public.module m
JOIN public.semester s ON s.semester_id = m.semester_id
JOIN public.module_student_group link ON link.module_id = m.module_id
JOIN public.student_group g ON g.group_id = link.group_id
WHERE m.module_code = 'ME1201' AND m.curriculum = 'C-23'
  AND s.semester_name = '1' AND s.academic_year = '2025/2026'
ORDER BY m.module_name;

SELECT m.module_id, m.module_code, m.module_name, m.curriculum
FROM public.event_requirement er
JOIN public.exam_requirement xr ON xr.requirement_id = er.requirement_id
JOIN public.module m ON m.module_id = er.module_id
JOIN public.calendar_period p ON p.period_id = er.period_id
WHERE p.period_name = 'Sprint 1 Dummy Exam Period - December 2026'
  AND er.requirement_type = 'EXAM' AND xr.has_exam
  AND NOT EXISTS (
    SELECT 1 FROM public.module_student_group link WHERE link.module_id = m.module_id
  )
ORDER BY m.module_id;
