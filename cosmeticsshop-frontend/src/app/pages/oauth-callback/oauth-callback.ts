import { isPlatformBrowser } from '@angular/common';
import { Component, inject, OnInit, PLATFORM_ID } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';

import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-oauth-callback-page',
  template: `
    <section class="oauth-callback">
      <p>Signing you in...</p>
    </section>
  `,
  styles: [`
    .oauth-callback {
      min-height: 100vh;
      display: grid;
      place-items: center;
      background: #f7f4ef;
      color: #151515;
      font: 600 1rem/1.5 system-ui, sans-serif;
    }
  `]
})
export class OAuthCallbackPage implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly authService = inject(AuthService);
  private readonly platformId = inject(PLATFORM_ID);

  ngOnInit(): void {
    if (!isPlatformBrowser(this.platformId)) {
      return;
    }

    const params = this.readOAuthParams();
    const token = this.firstParam(params, 'token');
    const refreshToken = this.firstParam(params, 'refreshToken');
    const email = this.firstParam(params, 'email');
    const role = this.firstParam(params, 'role');

    if (typeof token === 'string' && token.trim()) {
      this.authService.completeOAuthLogin({
        token,
        refreshToken: refreshToken ?? undefined,
        email: email ?? undefined,
        role: role ?? undefined
      });
      void this.router.navigate(['/dashboard'], { replaceUrl: true });
      return;
    }

    void this.router.navigate(['/login'], {
      queryParams: { oauthError: 'true' }
    });
  }

  private readOAuthParams(): URLSearchParams {
    const browserParams = this.readBrowserParams();
    if (browserParams.has('token')) {
      return browserParams;
    }

    const queryParams = new URLSearchParams();
    Object.entries(this.route.snapshot.queryParams).forEach(([key, value]) => {
      if (typeof value === 'string') {
        queryParams.set(key, value);
      }
    });

    if (queryParams.has('token')) {
      return queryParams;
    }

    const fragment = this.route.snapshot.fragment ?? '';
    return new URLSearchParams(fragment.startsWith('?') ? fragment.slice(1) : fragment);
  }

  private readBrowserParams(): URLSearchParams {
    if (typeof window === 'undefined') {
      return new URLSearchParams();
    }

    const params = new URLSearchParams(window.location.search);
    if (params.has('token')) {
      return params;
    }

    const hash = window.location.hash.replace(/^#\/?/, '');
    const queryStart = hash.indexOf('?');
    return queryStart >= 0 ? new URLSearchParams(hash.slice(queryStart + 1)) : new URLSearchParams();
  }

  private firstParam(params: URLSearchParams, key: string): string | null {
    const value = params.get(key);
    return value?.trim() || null;
  }
}
