import React from 'react';
import { SearchResult } from '../api/searchApi';

interface SearchResultsProps {
  results: SearchResult[];
  loading?: boolean;
  error?: string | null;
  onSelect: (result: SearchResult) => void;
}

export const SearchResults: React.FC<SearchResultsProps> = ({ results, loading = false, error, onSelect }) => {
  if (loading) {
    return <div className="p-3 text-sm text-slate-500">Searching...</div>;
  }

  if (error) {
    return <div className="rounded-md bg-red-50 p-3 text-sm text-red-700">{error}</div>;
  }

  if (results.length === 0) {
    return <div className="p-3 text-sm text-slate-500">No results.</div>;
  }

  return (
    <ul className="space-y-2">
      {results.map((result) => (
        <li key={result.messageId}>
          <button
            onClick={() => onSelect(result)}
            className="w-full rounded-md border border-slate-200 p-3 text-left hover:bg-slate-50"
          >
            <div className="mb-1 text-xs text-slate-500">
              {result.senderId} • {result.channelId} • {new Date(result.createdAt).toLocaleString()}
            </div>
            <div className="text-sm text-slate-800">{result.snippet}</div>
          </button>
        </li>
      ))}
    </ul>
  );
};
