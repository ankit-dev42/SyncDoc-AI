import React, { useCallback, useRef, useState } from 'react';
import { PresenceStatus } from './PresenceBadge';

interface PresenceStatusDropdownProps {
  currentStatus: PresenceStatus;
  onStatusChange: (status: PresenceStatus) => void;
}

const STATUS_OPTIONS: { value: PresenceStatus; label: string; colorClass: string }[] = [
  { value: 'ONLINE',  label: 'Online',       colorClass: 'bg-emerald-500' },
  { value: 'AWAY',    label: 'Away',         colorClass: 'bg-amber-500'   },
  { value: 'OFFLINE', label: 'Do not disturb', colorClass: 'bg-slate-400' },
];

export const PresenceStatusDropdown: React.FC<PresenceStatusDropdownProps> = ({
  currentStatus,
  onStatusChange,
}) => {
  const [open, setOpen] = useState(false);
  const buttonRef = useRef<HTMLButtonElement>(null);

  const toggle = useCallback(() => setOpen((prev) => !prev), []);

  const handleSelect = useCallback(
    (status: PresenceStatus) => {
      onStatusChange(status);
      setOpen(false);
      buttonRef.current?.focus();
    },
    [onStatusChange],
  );

  const handleKeyDown = useCallback(
    (e: React.KeyboardEvent<HTMLUListElement>) => {
      if (e.key === 'Escape') {
        setOpen(false);
        buttonRef.current?.focus();
      }
    },
    [],
  );

  const current = STATUS_OPTIONS.find((o) => o.value === currentStatus) ?? STATUS_OPTIONS[0];

  return (
    <div className="relative inline-block text-left">
      <button
        ref={buttonRef}
        type="button"
        aria-haspopup="listbox"
        aria-expanded={open}
        aria-label={`Presence status: ${current.label}`}
        className="inline-flex items-center gap-2 rounded-full px-3 py-1 text-sm font-medium
                   bg-slate-100 hover:bg-slate-200 focus:outline-none focus-visible:ring-2
                   focus-visible:ring-blue-500 text-slate-700"
        onClick={toggle}
      >
        <span className={`h-2.5 w-2.5 rounded-full ${current.colorClass}`} aria-hidden="true" />
        {current.label}
        <span className="ml-1 text-xs" aria-hidden="true">▾</span>
      </button>

      {open && (
        <ul
          role="listbox"
          aria-label="Set your presence status"
          tabIndex={-1}
          onKeyDown={handleKeyDown}
          className="absolute left-0 z-10 mt-1 w-44 rounded-lg border border-slate-200
                     bg-white shadow-lg focus:outline-none"
        >
          {STATUS_OPTIONS.map((option) => (
            <li
              key={option.value}
              role="option"
              aria-selected={option.value === currentStatus}
              tabIndex={0}
              className="flex cursor-pointer items-center gap-3 px-4 py-2 text-sm text-slate-700
                         hover:bg-slate-50 focus:bg-slate-50 focus:outline-none
                         aria-selected:font-semibold"
              onClick={() => handleSelect(option.value)}
              onKeyDown={(e) => {
                if (e.key === 'Enter' || e.key === ' ') {
                  e.preventDefault();
                  handleSelect(option.value);
                }
              }}
            >
              <span className={`h-2.5 w-2.5 rounded-full ${option.colorClass}`} aria-hidden="true" />
              {option.label}
            </li>
          ))}
        </ul>
      )}
    </div>
  );
};
