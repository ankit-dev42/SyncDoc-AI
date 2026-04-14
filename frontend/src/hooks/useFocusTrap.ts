import { useCallback, useEffect } from 'react';

/**
 * Traps focus inside `containerRef` while `active` is true.
 * Pressing Escape calls `onClose`.
 * Satisfies WCAG 2.1 SC 2.1.2 (No Keyboard Trap) and SC 2.1.1.
 */
export function useFocusTrap(
  containerRef: React.RefObject<HTMLElement | null>,
  active: boolean,
  onClose?: () => void,
) {
  const handleKeyDown = useCallback(
    (e: KeyboardEvent) => {
      if (!active || !containerRef.current) return;
      const container = containerRef.current;

      if (e.key === 'Escape') {
        onClose?.();
        return;
      }

      if (e.key !== 'Tab') return;

      const focusable = Array.from(
        container.querySelectorAll<HTMLElement>(
          'a[href], button:not([disabled]), input:not([disabled]), select:not([disabled]), ' +
          'textarea:not([disabled]), [tabindex]:not([tabindex="-1"])',
        ),
      );

      if (focusable.length === 0) return;

      const first = focusable[0];
      const last = focusable[focusable.length - 1];

      if (e.shiftKey && document.activeElement === first) {
        e.preventDefault();
        last.focus();
      } else if (!e.shiftKey && document.activeElement === last) {
        e.preventDefault();
        first.focus();
      }
    },
    [active, containerRef, onClose],
  );

  useEffect(() => {
    if (!active) return;
    document.addEventListener('keydown', handleKeyDown);
    return () => document.removeEventListener('keydown', handleKeyDown);
  }, [active, handleKeyDown]);
}

/**
 * Returns focus to `targetRef` when the component unmounts.
 * Use this after closing a modal/dropdown to restore keyboard position.
 */
export function useRestoreFocus(targetRef: React.RefObject<HTMLElement | null>) {
  useEffect(() => {
    const el = targetRef.current;
    return () => {
      el?.focus();
    };
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);
}
