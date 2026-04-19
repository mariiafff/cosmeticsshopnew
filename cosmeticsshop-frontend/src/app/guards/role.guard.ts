import { Injectable } from '@angular/core';
import {
  CanActivate,
  Router,
  ActivatedRouteSnapshot,
  RouterStateSnapshot,
  UrlTree
} from '@angular/router';
import { AuthService } from '../services/auth.service';

@Injectable({
  providedIn: 'root'
})
export class RoleGuard implements CanActivate {
  constructor(private readonly authService: AuthService, private readonly router: Router) {}

  canActivate(
    route: ActivatedRouteSnapshot,
    state: RouterStateSnapshot
  ): boolean | UrlTree {
    const allowedRoles = route.data['roles'] as string[] | string | undefined;
    const currentRole = this.authService.getRole();

    if (!allowedRoles || !currentRole) {
      return this.redirectToLogin(state.url);
    }

    const rolesArray = Array.isArray(allowedRoles) ? allowedRoles : [allowedRoles];
    if (rolesArray.includes(currentRole)) {
      return true;
    }

    return this.router.createUrlTree(['/dashboard']);
  }

  private redirectToLogin(returnUrl: string): UrlTree {
    return this.router.createUrlTree(['/login'], {
      queryParams: { returnUrl }
    });
  }
}
