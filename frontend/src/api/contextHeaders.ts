import { AxiosHeaders, InternalAxiosRequestConfig } from 'axios';

export function attachWorkspaceContextHeaders(config: InternalAxiosRequestConfig): InternalAxiosRequestConfig {
  const workspaceId = localStorage.getItem('workspaceId');
  const userId = localStorage.getItem('userId');

  const headers = AxiosHeaders.from(config.headers);
  if (workspaceId) {
    headers.set('X-Workspace-Id', workspaceId);
  }
  if (userId) {
    headers.set('X-User-Id', userId);
  }

  config.headers = headers;
  return config;
}