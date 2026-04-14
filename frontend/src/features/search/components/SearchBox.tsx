import React, { useState } from 'react';
import { SearchFilters } from '../api/searchApi';

interface SearchBoxProps {
  onSearch: (query: string, filters: SearchFilters) => void;
  isLoading?: boolean;
}

export const SearchBox: React.FC<SearchBoxProps> = ({ onSearch, isLoading = false }) => {
  const [query, setQuery] = useState('');
  const [filters, setFilters] = useState<SearchFilters>({});

  const updateFilter = (key: keyof SearchFilters, value: string) => {
    setFilters((prev) => ({ ...prev, [key]: value || undefined }));
  };

  return (
    <div className="space-y-2 rounded-md border border-slate-200 p-3">
      <div className="flex gap-2">
        <input
          value={query}
          onChange={(e) => setQuery(e.target.value)}
          placeholder="Search messages"
          className="flex-1 rounded-md border border-slate-300 px-3 py-2 text-sm"
        />
        <button
          onClick={() => onSearch(query, filters)}
          disabled={isLoading}
          className="rounded-md bg-slate-800 px-3 py-2 text-sm text-white disabled:opacity-60"
        >
          {isLoading ? 'Searching...' : 'Search'}
        </button>
      </div>

      <div className="grid grid-cols-1 gap-2 md:grid-cols-4">
        <input
          placeholder="from:userId"
          className="rounded-md border border-slate-300 px-2 py-1 text-xs"
          onChange={(e) => updateFilter('from', e.target.value)}
        />
        <input
          placeholder="in:channelId"
          className="rounded-md border border-slate-300 px-2 py-1 text-xs"
          onChange={(e) => updateFilter('in', e.target.value)}
        />
        <input
          placeholder="before:2026-04-14T00:00:00Z"
          className="rounded-md border border-slate-300 px-2 py-1 text-xs"
          onChange={(e) => updateFilter('before', e.target.value)}
        />
        <input
          placeholder="after:2026-04-01T00:00:00Z"
          className="rounded-md border border-slate-300 px-2 py-1 text-xs"
          onChange={(e) => updateFilter('after', e.target.value)}
        />
      </div>
    </div>
  );
};
