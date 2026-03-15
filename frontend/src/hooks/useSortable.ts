import { useState, useMemo } from 'react';

export type SortDirection = 'asc' | 'desc';

export interface SortConfig<K extends string> {
  key: K;
  direction: SortDirection;
}

export function useSortable<T, K extends string>(
  data: T[],
  defaultKey: K,
  defaultDirection: SortDirection = 'asc',
) {
  const [sortConfig, setSortConfig] = useState<SortConfig<K>>({
    key: defaultKey,
    direction: defaultDirection,
  });

  const handleSort = (key: K) => {
    setSortConfig((prev) =>
      prev.key === key
        ? { key, direction: prev.direction === 'asc' ? 'desc' : 'asc' }
        : { key, direction: 'asc' },
    );
  };

  const sortedData = useMemo(() => {
    const sorted = [...data];
    sorted.sort((a, b) => {
      const aVal = (a as Record<string, unknown>)[sortConfig.key];
      const bVal = (b as Record<string, unknown>)[sortConfig.key];

      if (aVal == null && bVal == null) return 0;
      if (aVal == null) return 1;
      if (bVal == null) return -1;

      let cmp = 0;
      if (typeof aVal === 'number' && typeof bVal === 'number') {
        cmp = aVal - bVal;
      } else {
        cmp = String(aVal).localeCompare(String(bVal), 'ko');
      }

      return sortConfig.direction === 'asc' ? cmp : -cmp;
    });
    return sorted;
  }, [data, sortConfig]);

  const getSortIndicator = (key: K): string => {
    if (sortConfig.key !== key) return ' ↕';
    return sortConfig.direction === 'asc' ? ' ↑' : ' ↓';
  };

  return { sortedData, sortConfig, handleSort, getSortIndicator };
}
