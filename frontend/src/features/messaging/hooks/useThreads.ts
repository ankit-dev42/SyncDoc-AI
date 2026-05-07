import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { threadApi, ThreadSummary } from '../api/threadApi';

export function useThreads(workspaceId: string, channelId: string) {
  const queryClient = useQueryClient();

  const result = useQuery<ThreadSummary[]>({
    queryKey: ['threads', workspaceId, channelId],
    queryFn: () => threadApi.listThreads(workspaceId, channelId),
    enabled: Boolean(workspaceId && channelId),
  });

  const createThread = useMutation({
    mutationFn: (message: string) =>
      threadApi.createThread(workspaceId, channelId, message),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['threads', workspaceId, channelId] });
    },
  });

  return { ...result, createThread };
}
