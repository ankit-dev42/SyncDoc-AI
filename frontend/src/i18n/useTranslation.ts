import { useCallback } from 'react';
import en from './en.json';

type TranslationDict = typeof en;

/** Flatten nested keys into dot-notation paths for type safety. */
type NestedKeyOf<T, Prefix extends string = ''> = {
  [K in keyof T & string]: T[K] extends Record<string, unknown>
    ? NestedKeyOf<T[K], `${Prefix}${K}.`>
    : `${Prefix}${K}`;
}[keyof T & string];

type TranslationKey = NestedKeyOf<TranslationDict>;

const LOCALES: Record<string, TranslationDict> = { en };

function resolvePath(obj: Record<string, unknown>, path: string): string | undefined {
  const parts = path.split('.');
  let current: unknown = obj;
  for (const part of parts) {
    if (current == null || typeof current !== 'object') return undefined;
    current = (current as Record<string, unknown>)[part];
  }
  return typeof current === 'string' ? current : undefined;
}

/**
 * Minimal, zero-dependency i18n hook.
 * Resolves dot-notation keys against the active locale bundle and performs
 * mustache-style interpolation: `{{variable}}`.
 */
export function useTranslation(locale: string = 'en') {
  const dict = (LOCALES[locale] ?? LOCALES['en']) as unknown as Record<string, unknown>;

  const t = useCallback(
    (key: TranslationKey, vars?: Record<string, string | number>): string => {
      const raw = resolvePath(dict, key) ?? key;
      if (!vars) return raw;
      return Object.entries(vars).reduce(
        (acc, [k, v]) => acc.replace(new RegExp(`\\{\\{${k}\\}\\}`, 'g'), String(v)),
        raw,
      );
    },
    [dict],
  );

  return { t };
}
