-- Create Room POST schema alignment
-- Run this script manually on Supabase/Postgres.

BEGIN;

-- 1) Per-room size (sqm)
ALTER TABLE public.rooms
  ADD COLUMN IF NOT EXISTS room_size_sqm numeric(6,2);

-- 2) Amenities should be text[] (if currently another array/json representation)
ALTER TABLE public.room_types
  ALTER COLUMN amenities TYPE text[]
  USING CASE
    WHEN amenities IS NULL THEN NULL
    ELSE amenities::text[]
  END;

-- 3) Constraints
ALTER TABLE public.rooms
  DROP CONSTRAINT IF EXISTS rooms_room_size_sqm_positive_chk;
ALTER TABLE public.rooms
  ADD CONSTRAINT rooms_room_size_sqm_positive_chk
  CHECK (room_size_sqm IS NULL OR room_size_sqm > 0);

ALTER TABLE public.room_types
  DROP CONSTRAINT IF EXISTS room_types_max_occupancy_chk;
ALTER TABLE public.room_types
  ADD CONSTRAINT room_types_max_occupancy_chk
  CHECK (max_occupancy BETWEEN 1 AND 20);

ALTER TABLE public.room_types
  DROP CONSTRAINT IF EXISTS room_types_base_price_chk;
ALTER TABLE public.room_types
  ADD CONSTRAINT room_types_base_price_chk
  CHECK (base_price > 0);

ALTER TABLE public.room_types
  DROP CONSTRAINT IF EXISTS room_types_discounted_price_non_negative_chk;
ALTER TABLE public.room_types
  ADD CONSTRAINT room_types_discounted_price_non_negative_chk
  CHECK (discounted_price IS NULL OR discounted_price >= 0);

ALTER TABLE public.room_types
  DROP CONSTRAINT IF EXISTS room_types_discount_not_exceed_base_chk;
ALTER TABLE public.room_types
  ADD CONSTRAINT room_types_discount_not_exceed_base_chk
  CHECK (discounted_price IS NULL OR discounted_price <= base_price);

-- 4) Keep gallery order unique per room type
DROP INDEX IF EXISTS room_type_images_room_type_id_sort_order_uidx;
CREATE UNIQUE INDEX room_type_images_room_type_id_sort_order_uidx
  ON public.room_type_images (room_type_id, sort_order);

COMMIT;
