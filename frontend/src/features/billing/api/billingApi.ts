import apiClient from '../../../api/client';

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
    const response = await apiClient.post<{ success: boolean; data: CheckoutSessionResponse }>(
      '/v1/billing/checkout',
      { userId },
    );
    return response.data.data;
  },

  /**
   * Retrieves the current subscription tier and status for the given user.
   * Delegates to the existing GET /api/subscriptions/{userId}/tier endpoint.
   */
  async getSubscriptionTier(userId: string): Promise<SubscriptionTierResponse> {
    const response = await apiClient.get<{ success: boolean; data: SubscriptionTierResponse }>(
      `/v1/subscriptions/${userId}/tier`,
    );
    return response.data.data;
  },
};
