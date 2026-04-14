import { create } from 'zustand';

type ChannelUnreadMap = Record<string, number>;

interface UnreadCountState {
  unreadByChannel: ChannelUnreadMap;
  setUnread: (channelId: string, count: number) => void;
  incrementUnread: (channelId: string) => void;
  clearUnread: (channelId: string) => void;
}

export const useUnreadCountStore = create<UnreadCountState>((set) => ({
  unreadByChannel: {},
  setUnread: (channelId, count) =>
    set((state) => ({ unreadByChannel: { ...state.unreadByChannel, [channelId]: count } })),
  incrementUnread: (channelId) =>
    set((state) => ({
      unreadByChannel: {
        ...state.unreadByChannel,
        [channelId]: (state.unreadByChannel[channelId] ?? 0) + 1,
      },
    })),
  clearUnread: (channelId) =>
    set((state) => ({ unreadByChannel: { ...state.unreadByChannel, [channelId]: 0 } })),
}));
