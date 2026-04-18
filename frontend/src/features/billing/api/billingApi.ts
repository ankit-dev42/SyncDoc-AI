import axios from 'axios';
import { attachWorkspaceContextHeaders } from '../../../api/contextHeaders';

const api = axios.create({ baseURL: 'http://localhost:8080/api/v1' });
api.interceptors.request.use((config) => attachWorkspaceContextHeaders(config));

export interface CheckoutSessionResponse {
  checkoutUrl: string;
  sessionId: string;
}

export interface SubscriptionTierResponse {
  userId: string;
  tier: string;
  status: string;
}

export const billingApi = {
  /**
   * Creates a Stripe checkout session for the given user and returns the
   * redirect URL to Stripe's hosted checkout page.
   */
  async createCheckoutSession(userId: string): Promise<CheckoutSessionResponse> {
    const response = await api.post<{ success: boolean; data: CheckoutSessionResponse }>(
      '/billing/checkout',
      { userId },
    );
    return response.data.data;
  },

  /**
   * Retrieves the current subscription tier and status for the given user.
   * Delegates to the existing GET /api/v1/subscriptions/{userId}/tier endpoint.
   */
  async getSubscriptionTier(userId: string): Promise<SubscriptionTierResponse> {
    const response = await api.get<{ success: boolean; data: SubscriptionTierResponse }>(
      `/subscriptions/${userId}/tier`,
    );
    return response.data.data;
  },
};
