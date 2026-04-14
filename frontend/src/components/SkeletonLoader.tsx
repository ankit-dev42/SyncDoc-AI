import React from 'react';

interface SkeletonLoaderProps {
  /** Number of skeleton rows to render (default: 3) */
  rows?: number;
  /** Extra Tailwind classes applied to each row */
  className?: string;
  /** Accessible label for screen readers */
  label?: string;
}

/**
 * Animated placeholder shown while content is loading.
 * WCAG 2.1 AA: uses role="status" + aria-label so screen readers
 * announce that content is loading without reading out each row.
 */
export const SkeletonLoader: React.FC<SkeletonLoaderProps> = ({
  rows = 3,
  className = '',
  label = 'Loading…',
}) => {
  return (
    <div role="status" aria-label={label} className="w-full space-y-3">
      {Array.from({ length: rows }).map((_, i) => (
        <div
          key={i}
          aria-hidden="true"
          className={`h-4 w-full animate-pulse rounded-md bg-slate-200 ${className}`}
          style={{ opacity: 1 - i * 0.15 }}
        />
      ))}
      <span className="sr-only">{label}</span>
    </div>
  );
};

/** Compact single-line skeleton used inside lists */
export const SkeletonRow: React.FC<{ width?: string }> = ({ width = 'w-full' }) => (
  <div aria-hidden="true" className={`h-3 ${width} animate-pulse rounded bg-slate-200`} />
);

/** Square / circle avatar skeleton */
export const SkeletonAvatar: React.FC<{ size?: number }> = ({ size = 8 }) => (
  <div
    aria-hidden="true"
    className={`h-${size} w-${size} animate-pulse rounded-full bg-slate-200`}
  />
);
