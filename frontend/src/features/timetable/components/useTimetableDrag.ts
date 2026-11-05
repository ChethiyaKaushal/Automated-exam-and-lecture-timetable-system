import { useEffect, useRef, useState } from "react";
import type { PointerEvent as ReactPointerEvent } from "react";

export type TimetableTarget = { kind: "move" | "swap"; id: number };

type Gesture = { pointerId: number; source: number; x: number; y: number; moved: boolean };

function targetAt(x: number, y: number, source: number): TimetableTarget | null {
  const element = document.elementFromPoint(x, y)?.closest<HTMLElement>("[data-move-to], [data-swap-with]");
  if (!element) return null;
  if (element.dataset.moveTo) return { kind: "move", id: Number(element.dataset.moveTo) };
  const id = Number(element.dataset.swapWith);
  return id === source ? null : { kind: "swap", id };
}

function scrollSpeed(position: number, start: number, end: number) {
  const edge = 44;
  if (position < start + edge) return Math.max(-16, (position - start - edge) / 3);
  if (position > end - edge) return Math.min(16, (position - end + edge) / 3);
  return 0;
}

export function useTimetableDrag(onSelect: (id: number) => void, onDrop: (source: number, target: TimetableTarget) => void) {
  const [target, setTarget] = useState<TimetableTarget | null>(null);
  const [dragging, setDragging] = useState(false);
  const gesture = useRef<Gesture | null>(null);
  const start = useRef({ x: 0, y: 0 });
  const frame = useRef(0);
  const scroller = useRef<HTMLDivElement>(null);

  useEffect(() => () => cancelAnimationFrame(frame.current), []);

  function tick() {
    const active = gesture.current;
    if (!active) return;
    const dy = scrollSpeed(active.y, 64, window.innerHeight);
    if (dy) window.scrollBy(0, dy);
    const bounds = scroller.current?.getBoundingClientRect();
    if (bounds && active.y >= bounds.top && active.y <= bounds.bottom) {
      const dx = scrollSpeed(active.x, bounds.left, bounds.right);
      if (dx) scroller.current?.scrollBy(dx, 0);
    }
    const next = targetAt(active.x, active.y, active.source);
    setTarget(previous => previous?.id === next?.id && previous?.kind === next?.kind ? previous : next);
    frame.current = requestAnimationFrame(tick);
  }

  function cancel(event: ReactPointerEvent<HTMLButtonElement>) {
    if (gesture.current?.pointerId !== event.pointerId) return;
    gesture.current = null;
    cancelAnimationFrame(frame.current);
    setDragging(false);
    setTarget(null);
  }

  function bind(source: number) {
    return {
      onPointerDown(event: ReactPointerEvent<HTMLButtonElement>) {
        if (!event.isPrimary || event.button !== 0 || gesture.current) return;
        const { pointerId, clientX: x, clientY: y } = event;
        start.current = { x, y };
        gesture.current = { pointerId, source, x, y, moved: false };
        event.currentTarget.setPointerCapture(pointerId);
      },
      onPointerMove(event: ReactPointerEvent<HTMLButtonElement>) {
        const active = gesture.current;
        if (!active || active.pointerId !== event.pointerId) return;
        active.x = event.clientX;
        active.y = event.clientY;
        if (!active.moved && Math.hypot(active.x - start.current.x, active.y - start.current.y) >= 7) {
          active.moved = true;
          setDragging(true);
          frame.current = requestAnimationFrame(tick);
        }
      },
      onPointerUp(event: ReactPointerEvent<HTMLButtonElement>) {
        const active = gesture.current;
        if (!active || active.pointerId !== event.pointerId) return;
        cancel(event);
        if (!active.moved) {
          onSelect(source);
          return;
        }
        const destination = targetAt(event.clientX, event.clientY, source);
        if (destination) onDrop(source, destination);
      },
      onPointerCancel: cancel,
      onLostPointerCapture: cancel,
      onClick(event: React.MouseEvent<HTMLButtonElement>) {
        if (event.detail === 0) onSelect(source);
      },
    };
  }

  return { bind, dragging, target, scroller };
}
