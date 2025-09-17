ALTER TABLE public.analysis
  ALTER COLUMN data TYPE text
  USING data::text;